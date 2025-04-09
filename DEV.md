
```sh

./mill clean

./mill __.reformat

./mill __.test

scala-cli compile examples\scala-cli

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal
```

```sh

# RELEASE
$VERSION="0.9.3"
git commit --allow-empty -m "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push --atomic origin main --tags

```

# TODOs

- make sure / always returns 404 by default. for some reason 403 is returned...

- MiMa bin compat

- giter8 template for REST

- add more validators https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-summary.html

README DEMO:

https://carbon.now.sh/?bg=rgba%28171%2C+184%2C+195%2C+1%29&t=a11y-dark&wt=bw&l=text%2Fx-scala&width=800&ds=true&dsyoff=38px&dsblur=61px&wc=true&wa=false&pv=56px&ph=61px&ln=false&fl=1&fm=Hack&fs=14px&lh=133%25&si=false&es=4x&wm=false&code=%252F*%2520%7Eeveryhing%2520is%2520a%2520case%2520class%2520mantra%2520*%252F%250A%250A%252F%252F%2520JSON%2520request%2520body%250Acase%2520class%2520Car%28model%253A%2520String%252C%2520quantity%253A%2520Int%29%2520derives%2520JsonRW%250A%250A%252F%252F%2520typesafe%2520query%2520parameters%250Acase%2520class%2520CarQuery%28model%253A%2520String%2520%253D%2520%2522Yugo%2522%29%2520derives%2520QueryStringRW%250A%250A%252F%252F%2520exhaustive%2520pattern%2520matching%2520for%2520routes%250Aval%2520routes%2520%253D%2520Routes%253A%250A%2520%2520case%2520GET%2520-%253E%2520Path%28%2522cars%2522%29%2520%253D%253E%250A%2520%2520%2520%2520val%2520qp%2520%253D%2520Request.current.queryParamsValidated%255BCarQuery%255D%250A%2520%2520%2520%2520val%2520filteredCars%2520%253D%2520carsDb.getByModel%28qp.model%29%250A%2520%2520%2520%2520Response.withBody%28filteredCars%29%250A%250A%2520%2520case%2520POST%2520-%253E%2520Path%28%2522cars%2522%29%2520%253D%253E%250A%2520%2520%2520%2520val%2520newCar%2520%253D%2520Request.current.bodyJsonValidated%255BCar%255D%250A%2520%2520%2520%2520carsDB.insert%28newCar%29%250A%2520%2520%2520%2520Response.withBody%28newCar%29

