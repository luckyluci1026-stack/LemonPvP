/** Wendet das Datenbankschema an (wird auch beim Serverstart automatisch ausgeführt). */
import { pool, migrate } from "./db.js";

await migrate({ info: (m) => console.log(`✓ ${m}`) });
await pool.end();
