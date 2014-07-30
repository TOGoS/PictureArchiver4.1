tjbuilder = java -jar util/TJBuilder.jar
touch = ${tjbuilder} touch

default: PA4.jar

.PHONY: default clean .FORCE
.DELETE_ON_ERROR:

clean:
	rm -rf bin PA4.jar .src.lst

src: .FORCE
	${touch} -latest-within src -latest-within ext-lib src

PA4.jar: src
	rm -rf bin PA4.jar
	cp -r src bin
	find src -name *.java >.src.lst
	javac -source 1.6 -target 1.6 -d bin @.src.lst
	mkdir -p bin/META-INF
	echo 'Version: 1.0' >bin/META-INF/MANIFEST.MF
	echo 'Main-Class: togos.picturearchiver4_1.PAMainWindow' >>bin/META-INF/MANIFEST.MF
	cd bin ; zip -r ../PA4.jar . ; cd ..
