# LanDrop Protocol v1

## Discovery

- UDP port: `40404`
- Every three seconds each peer broadcasts UTF-8 JSON:

```json
{"app":"LanDrop","version":1,"name":"Living-room PC","platform":"windows","port":40405}
```

Receivers derive the IP address from the UDP datagram source and expire peers after 10 seconds.

## HTTP

Server listens on `0.0.0.0:40405`.

- `GET /api/info` returns the discovery JSON.
- `POST /api/files` accepts `multipart/form-data`; form field name is `file`.
- Successful upload returns HTTP `201` with `{ "ok": true, "name": "...", "size": 123 }`.

File names are reduced to their final path component and invalid file-name characters are replaced.
