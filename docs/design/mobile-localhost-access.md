# Access IMS on a real phone (local testing)

Use this when checking the Angular shell on a physical phone or tablet on the **same Wi‑Fi** as your PC.

## Why `localhost` on the phone does not work

On the phone, `http://localhost:4200` means **the phone itself**, not your PC.

You must open the UI via your PC’s LAN IP, for example:

`http://192.168.1.42:4200`

Also, the UI’s API base URL must point at the **PC**, not `localhost`. If `environment.ts` still has `http://localhost:8088`, the phone will call its own port 8088 and login/API calls will fail.

---

## 1. Find your PC’s LAN IP (Windows)

In PowerShell:

```powershell
ipconfig
```

Look under your active Wi‑Fi / Ethernet adapter for **IPv4 Address**, e.g. `192.168.1.42`.

Use that value everywhere below as `PC_IP`.

---

## 2. Start services bound for LAN access

### Infrastructure (if needed)

```powershell
cd D:\workspace\working\institute-management-system\infra
docker compose up -d
```

(MinIO / Jaeger; MySQL may be your local install.)

### Backend

```powershell
cd D:\workspace\working\institute-management-system\ims-api
mvn -pl ims-application spring-boot:run
# other terminal
mvn -pl api-gateway spring-boot:run
```

Gateway listens on **8088**; app on **8080**.

### Frontend (important: host `0.0.0.0`)

```powershell
cd D:\workspace\working\institute-management-system\ims-ui
npx ng serve --host 0.0.0.0 --port 4200
```

`--host 0.0.0.0` allows other devices on the network to connect. Serving only on `localhost` will not be reachable from the phone.

---

## 3. Point the UI API at your PC IP

Edit `ims-ui/src/environments/environment.ts` **temporarily** for phone testing:

```ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://PC_IP:8088', // e.g. http://192.168.1.42:8088
};
```

Revert to `http://localhost:8088` when you are back on desktop-only testing.

Restart / let `ng serve` reload after the change.

---

## 4. Allow the phone origin in gateway CORS

The browser on the phone sends `Origin: http://PC_IP:4200`. That is **not** `localhost`, so gateway CORS must allow it.

In `ims-api/api-gateway/src/main/resources/application.yml`, under `globalcors.cors-configurations['[/**]'].allowedOriginPatterns`, include LAN patterns, for example:

```yaml
allowedOriginPatterns:
  - "http://localhost:*"
  - "http://127.0.0.1:*"
  - "http://192.168.*:*"
  - "http://10.*:*"
```

Restart **api-gateway** after changing this.

---

## 5. Windows Firewall

Your Wi‑Fi may be classified as **Public** (common). Inbound Node rules often exist, but an explicit port rule is more reliable.

### Open ports (run PowerShell **as Administrator**)

```powershell
New-NetFirewallRule -DisplayName 'IMS ng-serve 4200' -Direction Inbound -Protocol TCP -LocalPort 4200 -Action Allow -Profile Any
New-NetFirewallRule -DisplayName 'IMS api-gateway 8088' -Direction Inbound -Protocol TCP -LocalPort 8088 -Action Allow -Profile Any
```

Optional: mark the Wi‑Fi as Private (Settings → Network → your Wi‑Fi → **Private network**).

### Quick isolation test

Temporarily turn off the firewall for Private/Public profiles, retry the phone URL, then turn it back on:

```powershell
# Admin PowerShell — TEST ONLY
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled False
# ... try phone again ...
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled True
```

If it works only with the firewall off, the port rules above are required.

Quick check from the phone’s browser: open `http://PC_IP:8088/actuator/health` — you should see JSON (gateway must be running).

---

## 5b. Router “AP isolation” (very common on Xiaomi / `192.168.31.x`)

`ERR_CONNECTION_FAILED` with the correct IP often means the **router blocks phone ↔ PC traffic**.

On the router admin page (often `http://192.168.31.1`):

- Disable **AP isolation** / **Wireless isolation** / **Client isolation** / **Guest network**
- Put phone and PC on the **same SSID** (not Guest)
- Avoid VPN on phone or PC while testing

Then retry `http://192.168.31.123:4200` on the phone.

### Fallback: USB debugging (Android)

If Wi‑Fi isolation cannot be disabled:

```powershell
adb reverse tcp:4200 tcp:4200
adb reverse tcp:8088 tcp:8088
```

On the phone open `http://127.0.0.1:4200` (traffic tunnels to the PC). Keep `apiBaseUrl` as `http://127.0.0.1:8088` for that session.

---

## 6. Open on the phone

1. Join the **same Wi‑Fi** as the PC (guest/isolated Wi‑Fi often blocks device-to-device traffic).
2. In the phone browser open: `http://PC_IP:4200/login`
3. Sign in: `admin` / `Password@123` / `DEMO_A`

Suggested checks: drawer open/close, no horizontal page scroll, login keyboard flow (see [responsive-phase1-smoke.md](./responsive-phase1-smoke.md)).

---

## Emulators / simulators (optional)

| Environment | UI URL | API tip |
| --- | --- | --- |
| **Android Emulator** | Often `http://10.0.2.2:4200` | `10.0.2.2` is the emulator alias for the host PC’s localhost |
| **iOS Simulator** | `http://localhost:4200` | Shares the Mac network stack; `localhost` is the Mac |
| **Physical phone** | `http://PC_IP:4200` | Use LAN IP as above |

---

## Checklist

- [ ] PC and phone on same Wi‑Fi
- [ ] `ng serve --host 0.0.0.0 --port 4200`
- [ ] `environment.apiBaseUrl` = `http://PC_IP:8088`
- [ ] Gateway CORS allows `http://192.168.*:*` (and/or `http://10.*:*`)
- [ ] Firewall allows 4200 and 8088
- [ ] Phone can open `http://PC_IP:4200/login` and login succeeds

---

## Common failures

| Symptom | Likely cause |
| --- | --- |
| Page never loads (`ERR_CONNECTION_FAILED`) | Firewall blocking 4200; **router AP isolation**; phone on Guest Wi‑Fi; wrong IP (ignore `172.20.*` WSL/Hyper-V adapters) |
| Page loads, login CORS / network error | `apiBaseUrl` still `localhost`, or CORS missing LAN origin |
| Health works, UI blank | Wrong port or HTTPS/HTTP mixed content (use `http://` for local) |
| Works on PC, not on phone | Phone on different VLAN/guest Wi‑Fi |

When finished testing, set `apiBaseUrl` back to `http://localhost:8088` for normal desktop development.
