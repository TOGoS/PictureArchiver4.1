.PHONY: all clean

all: PA4.jar

clean:
	rm -rf bin PA4.jar .src.lst

PA4.jar:
	rm -rf bin PA4.jar
	cp -r src bin
	find src -name *.java >.src.lst
	javac -source 1.6 -target 1.6 -d bin @.src.lst
	mkdir -p bin/META-INF
	echo 'Version: 1.0' >bin/META-INF/MANIFEST.MF
	echo 'Main-Class: togos.picturearchiver4_1.PAMainWindow' >>bin/META-INF/MANIFEST.MF
	cd bin ; zip -r ../PA4.jar . ; cd ..
