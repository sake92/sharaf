
```sh

./mill clean

./mill __.reformat

./mill __.test

scala-cli compile examples\scala-cli

./mill examples.runMain bla

# for local dev/test
./mill __.publishLocal

git diff
git commit -am "msg"

$VERSION="0.7.3"
git commit --allow-empty -m "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push  --atomic origin main $VERSION


```

# TODOs

- MiMa bin compat

- giter8 template for REST
- add more validators https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-summary.html
- webjars


README DEMO:

https://carbon.now.sh/?bg=rgba%28171%2C+184%2C+195%2C+1%29&t=a11y-dark&wt=bw&l=text%2Fx-scala&width=800&ds=true&dsyoff=38px&dsblur=61px&wc=true&wa=false&pv=56px&ph=61px&ln=false&fl=1&fm=Hack&fs=14px&lh=133%25&si=false&es=4x&wm=false&code=case%2520class%2520Car%28model%253A%2520String%252C%2520quantity%253A%2520Int%29%2520derives%2520JsonRW%250A%250Acase%2520class%2520CarQuery%28model%253A%2520Option%255BString%255D%29%2520derives%2520QueryStringRW%250A%250Avar%2520carsDB%2520%253D%2520Seq%255BCar%255D%28%29%250A%250Aval%2520routes%2520%253D%2520Routes%253A%250A%2520%2520case%2520GET%28%29%2520-%253E%2520Path%28%2522cars%2522%29%2520%253D%253E%250A%2520%2520%2520%2520val%2520qp%2520%253D%2520Request.current.queryParamsValidated%255BCarQuery%255D%250A%2520%2520%2520%2520val%2520filteredCars%2520%253D%2520qp.model%2520match%250A%2520%2520%2520%2520%2520%2520case%2520Some%28b%29%2520%253D%253E%2520carsDB.filter%28_.model%2520%253D%253D%2520b%29%250A%2520%2520%2520%2520%2520%2520case%2520None%2520%2520%2520%2520%253D%253E%2520carsDB%250A%2520%2520%2520%2520Response.withBody%28filteredCars%29%250A%250A%2520%2520case%2520POST%28%29%2520-%253E%2520Path%28%2522cars%2522%29%2520%253D%253E%250A%2520%2520%2520%2520val%2520newCar%2520%253D%2520Request.current.bodyJsonValidated%255BCar%255D%250A%2520%2520%2520%2520carsDB%2520%253D%2520carsDB.appended%28newCar%29%250A%2520%2520%2520%2520Response.withBody%28newCar%29

