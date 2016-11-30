# vimsical-sim

A Clojure library designed to ... well, that part is up to you.

## Usage

FIXME

## Performance knobs
### Immutant
See Jim Crossley's comment: https://groups.google.com/forum/#!topic/clojure/nhz7ADiK7Fs

### Websocket
Pipeline parallelism

### Cassandra
### Datomic

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

```
curl -i -N \
     -H "Connection:Upgrade" \
     -H "Sec-WebSocket-Key: rk6KDgfg0GWCih5G/XRiDg==" \
     -H "Sec-WebSocket-Version: 13" \
     -H "Upgrade: websocket" \
     -H "Host: localhost:8080" \
     -H "Origin: http://localhost:8080" \
     http://localhost:8080/websocket?client-id=fc391090-eb82-4585-91fa-7010d70685ba
```
