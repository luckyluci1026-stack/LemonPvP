# paperswap.studio

The PaperSwap website: one static page (`index.html`) with its images (`assets/`) and fonts
(`fonts/`). No build step, no JavaScript, no external requests (no CDN, no Google Fonts, no
analytics), so visitors' browsers only talk to the server that hosts the page.

## Preview locally

Open `index.html` in a browser, or serve the folder:

```bash
cd website
python3 -m http.server 8000   # http://localhost:8000
```

## Host it

Any static host works; publish the `website/` folder as the site root.

- **Cloudflare Pages / Netlify:** connect the repository, leave the build command empty and set
  the output (publish) directory to `website`. Then add `paperswap.studio` as a custom domain in
  the host's dashboard and follow its DNS instructions.
- **GitHub Pages:** branch deployments only serve the repository root or `docs/`, so use a
  workflow: in *Settings → Pages* choose *GitHub Actions* as the source, and add a workflow that
  uploads `website/` with `actions/upload-pages-artifact` and publishes it with
  `actions/deploy-pages` (see GitHub's
  [custom workflow guide](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages)).
  Set the custom domain in *Settings → Pages* and configure DNS as described in
  [Managing a custom domain](https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/managing-a-custom-domain-for-your-github-pages-site).

## Before going live

- **Legal notice:** if the site is operated from Germany, it may need an *Impressum* (and,
  depending on hosting and logging, a privacy notice). The page has no such page yet, because it
  needs the operator's own details.
- **Social preview:** `og:image` points to `https://paperswap.studio/assets/paperswap-social-preview.png`;
  it works once the site is served from that domain.

## Content rules

- The console excerpt in "How a live update works" is real output from the CI job
  "Spear fix as live core patch (1.21.11)"; omitted lines are marked `⋮`. Replace it only with
  real output.
- Claims on the page follow the README and `docs/LIMITATIONS.md`. When behaviour changes,
  update both.

## Credits

- Fonts: [Bricolage Grotesque](https://github.com/ateliertriay/bricolage) and
  [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), both under the SIL Open Font
  License 1.1 (`fonts/OFL-*.txt`), taken from the Fontsource packages.
- Swappie and the logo are part of this repository (GPL-3.0).
