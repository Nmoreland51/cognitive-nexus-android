# Use the same private backend at home and away

Status: supported by the native HTTPS client, **not deployed or paired yet**. No public port forwarding,
VPN installation, Tailscale account login, or remote-access configuration has been performed by this change.

## Private HTTPS with Tailscale Serve

1. Install Tailscale on the computer and Android phone. Sign both into the same account/private network.
2. Keep Ollama, the Mobile API v2 adapter, and the computer running. The phone still depends on this computer;
   sleep/shutdown or lost internet stops remote AI features.
3. Start the adapter using the README instructions. Leave its binding at `127.0.0.1:8000`.
   Generate and privately retain `NEXUS_API_TOKEN`; this is separate from any LLM provider key.
4. On the computer, after reviewing any existing Serve configuration:

   ```powershell
   tailscale serve status
   tailscale serve --bg http://127.0.0.1:8000
   ```

   If prompted, approve HTTPS in your Tailscale account. Use the **actual HTTPS address printed by
   Tailscale**—do not invent a machine name or copy an example hostname.
5. In Cognitive Nexus Mobile → Settings, enter that HTTPS URL and the backend access token.
   Keep Tailscale connected on the phone, both on home Wi-Fi and on mobile data.
6. Test home Wi-Fi first: connect, refresh live providers, send a short message, open Gallery.
   Then disable Wi-Fi on the phone and repeat over cellular data. Both must pass before remote access
   is considered verified.

Serve is private to your Tailscale network. **Do not enable Funnel**, which is for public exposure.
Keep Tailscale access rules limited to devices/users you trust: this adapter shares one owner's
knowledge, files, memory, and model usage. Keep its access token secret and rotate it if exposed.
Review the existing Serve setup before changing it so another private service is not overwritten.

HTTPS certificate issuance can publish the machine's certificate name in public certificate-transparency
logs; choose a non-sensitive machine name. The private network still controls access to the service.

Official references: [Tailscale Serve](https://tailscale.com/docs/features/tailscale-serve),
[Serve command](https://tailscale.com/docs/reference/tailscale-cli/serve),
[HTTPS setup](https://tailscale.com/docs/how-to/set-up-https-certificates).

## Existing hosted HTTPS backend

An already configured HTTPS reverse proxy is another option. It must route the implemented `/api/`
paths to this adapter, preserve the Authorization header, and retain token authentication and suitable
network/access restrictions. Merely entering a URL does not deploy the engine or download models.

## Troubleshooting

- `401` / access denied: check the token; the server's token must match the phone's saved token.
- Old backend/version error: the URL is pointing at the old demo API or Streamlit, not Mobile API v2.
- Cannot connect remotely: check both devices' private-network connection, access rules, HTTPS address,
  and whether the computer/adapter is still running.
- Connected, but no model: start/load an installed Ollama model or configure a server-side provider.
- Image provider unavailable: configure Automatic1111, Diffusers, or a saved ComfyUI workflow; the app
  does not manufacture a replacement image.
