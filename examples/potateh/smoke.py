"""Run against the linked example: python3 examples/potateh/smoke.py <binary>."""

import http.client
import json
import pathlib
import queue
import socket
import subprocess
import sys
import threading


def main():
    binary = pathlib.Path(sys.argv[1]).resolve()
    lines = queue.Queue()
    process = subprocess.Popen(
        [str(binary), "0"], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True,
    )

    def read_output():
        for line in process.stdout:
            lines.put(line.rstrip())

    reader = threading.Thread(target=read_output)
    reader.start()
    try:
        line = lines.get(timeout=30)
        assert line.startswith("LISTENING "), line
        port = int(line.split()[1])
        connection = http.client.HTTPConnection("127.0.0.1", port, timeout=5)

        def request(method, path, body=None, headers=None, chunked=False):
            connection.request(method, path, body, headers or {}, encode_chunked=chunked)
            response = connection.getresponse()
            data = response.read()
            return response, data

        try:
            response, body = request("GET", "/hello/%C4%8D+%26")
            assert response.status == 200
            assert body.decode() == "Hello, č+&!", body
            assert response.getheader("Content-Type") == "text/plain; charset=utf-8"

            response, body = request("GET", "/query?q=a%26b&q=%252B&q=hello+world&q=%2B")
            assert response.status == 200
            assert body == b"a&b|%2B|hello world|+", body

            response, body = request("POST", "/json", json.dumps({"text": "čćž"}),
                                     {"Content-Type": "application/json"})
            assert response.status == 200
            assert json.loads(body) == {"text": "Received: čćž"}
            assert response.getheader("Content-Type") == "application/json; charset=utf-8"

            response, body = request("POST", "/form", "name=Alice+Smith&tags=a%26b&tags=%2B",
                                     {"Content-Type": "application/x-www-form-urlencoded; charset=utf-8"})
            assert response.status == 200
            assert body == b"Alice Smith:a&b|+", body

            # The same persistent connection carries all requests, including a chunked body.
            response, body = request("POST", "/echo", iter([b"hello ", "čćž".encode()]), chunked=True)
            assert response.status == 200
            assert body == "hello čćž".encode(), body

            connection.putrequest("GET", "/headers")
            connection.putheader("X-Demo", "one")
            connection.putheader("x-demo", "two")
            connection.putheader("Cookie", "a=1; b=x=y")
            connection.endheaders()
            response = connection.getresponse()
            assert response.read() == b"one|two;a=1|b=x=y"
            assert response.status == 200
            headers = response.getheaders()
            assert [v for k, v in headers if k.lower() == "x-repeat"] == ["one", "two"]
            assert response.getheader("X-Removed") is None
            assert response.getheader("Content-Type") == "text/custom"
            cookies = [v for k, v in headers if k.lower() == "set-cookie"]
            assert cookies[:2] == ["first=one; Path=/; HttpOnly; SameSite=Lax", "second=two"], cookies
            assert len(cookies) == 3 and cookies[2].startswith("old=; Expires="), cookies

            response, body = request("GET", "/stream")
            assert response.status == 200
            assert body == "streamed čćž".encode()
            assert response.getheader("Transfer-Encoding") == "chunked"

            response, body = request("GET", "/empty")
            assert response.status == 204 and body == b""
            response, body = request("GET", "/missing")
            assert response.status == 404 and body == b"Not Found"
            response, _ = request("POST", "/json", "{", {"Content-Type": "application/json"})
            assert response.status == 400
            response, body = request("GET", "/fail")
            assert response.status == 500 and body == b"Server error"
        finally:
            connection.close()

        process.stdin.write("\n")
        process.stdin.flush()
        assert process.wait(timeout=10) == 0
        with socket.socket() as probe:
            probe.settimeout(1)
            assert probe.connect_ex(("127.0.0.1", port)) != 0, "server still listening"
        print("PASS: Native routing, query, JSON, forms, chunked echo, headers, cookies, streaming, errors, shutdown")
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
        reader.join(timeout=5)
        process.stdin.close()
        process.stdout.close()
        if sys.exc_info()[0] is not None:
            while not lines.empty():
                print(lines.get_nowait(), file=sys.stderr)


if __name__ == "__main__":
    main()
