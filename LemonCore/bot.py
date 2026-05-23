"""
LemonPvP Discord Bot — handles /verify for the Minecraft Discord link system.

Setup:
  1. Copy bot_config.yml.example to bot_config.yml and fill in your values.
  2. pip install -r requirements.txt
  3. python bot.py
"""

import discord
from discord import app_commands
import mysql.connector
import yaml
from datetime import datetime

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

with open("bot_config.yml", "r") as f:
    config = yaml.safe_load(f)

TOKEN = config["bot"]["token"]
GUILD_ID = int(config["bot"]["guild_id"])

DB_CONFIG = {
    "host": config["database"]["host"],
    "port": int(config["database"].get("port", 3306)),
    "database": config["database"]["database"],
    "user": config["database"]["username"],
    "password": config["database"]["password"],
    "charset": "utf8mb4",
    "autocommit": False,
}


def get_db():
    return mysql.connector.connect(**DB_CONFIG)


# ---------------------------------------------------------------------------
# Bot
# ---------------------------------------------------------------------------

class LemonBot(discord.Client):
    def __init__(self):
        super().__init__(intents=discord.Intents.default())
        self.tree = app_commands.CommandTree(self)

    async def setup_hook(self):
        guild = discord.Object(id=GUILD_ID)
        self.tree.copy_global_to(guild=guild)
        await self.tree.sync(guild=guild)
        print(f"[LemonBot] Slash commands synced to guild {GUILD_ID}")

    async def on_ready(self):
        print(f"[LemonBot] Logged in as {self.user} (ID: {self.user.id})")


client = LemonBot()


# ---------------------------------------------------------------------------
# /verify command
# ---------------------------------------------------------------------------

@client.tree.command(name="verify", description="Link your Minecraft account to Discord")
@app_commands.describe(code="The 6-character code shown by /link ingame")
async def verify(interaction: discord.Interaction, code: str):
    code = code.upper().strip()

    if len(code) != 6:
        await interaction.response.send_message(
            "❌ Invalid code format. Use `/link` ingame to get a fresh code.",
            ephemeral=True,
        )
        return

    db = None
    cursor = None
    try:
        db = get_db()
        cursor = db.cursor(dictionary=True)

        # Look up the code
        cursor.execute(
            "SELECT uuid, player_name, expires_at FROM lc_verify_codes WHERE code = %s",
            (code,),
        )
        row = cursor.fetchone()

        if row is None:
            await interaction.response.send_message(
                "❌ Code not found. Use `/link` ingame to get a new one.",
                ephemeral=True,
            )
            return

        if row["expires_at"] < datetime.now():
            cursor.execute("DELETE FROM lc_verify_codes WHERE code = %s", (code,))
            db.commit()
            await interaction.response.send_message(
                "❌ Code expired. Use `/link` ingame to get a fresh one.",
                ephemeral=True,
            )
            return

        mc_uuid = row["uuid"]
        mc_name = row["player_name"]
        discord_id = str(interaction.user.id)
        discord_username = interaction.user.name

        # Check if this Discord account is already linked to a different player
        cursor.execute(
            "SELECT uuid FROM lc_discord_links WHERE discord_id = %s",
            (discord_id,),
        )
        existing = cursor.fetchone()
        if existing and existing["uuid"] != mc_uuid:
            await interaction.response.send_message(
                "❌ Your Discord account is already linked to a different Minecraft account. "
                "Ask an admin or use `/unlink` ingame first.",
                ephemeral=True,
            )
            return

        # Write the link (INSERT ... ON DUPLICATE KEY UPDATE for re-links)
        cursor.execute(
            """
            INSERT INTO lc_discord_links (uuid, discord_id, discord_username, linked_at)
            VALUES (%s, %s, %s, NOW())
            ON DUPLICATE KEY UPDATE
                discord_id = VALUES(discord_id),
                discord_username = VALUES(discord_username),
                linked_at = NOW()
            """,
            (mc_uuid, discord_id, discord_username),
        )

        # Remove the consumed code
        cursor.execute("DELETE FROM lc_verify_codes WHERE code = %s", (code,))
        db.commit()

        await interaction.response.send_message(
            f"✅ Successfully linked to **{mc_name}**!\n"
            f"You will receive a confirmation message ingame within a few seconds.",
            ephemeral=True,
        )

    except Exception as e:
        print(f"[LemonBot] Error in /verify: {e}")
        if db:
            try:
                db.rollback()
            except Exception:
                pass
        await interaction.response.send_message(
            "❌ An internal error occurred. Please try again later.",
            ephemeral=True,
        )
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

client.run(TOKEN)
