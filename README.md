# PictureArchiver 4.1

A utility to help import ('archive') pictures from
another directory mirroring the structure of the destination one,
with tools to rotate, flip, and compress pictures.

Idea is you dump all your incoming JPEGs into a structure
that mirrors that of an 'archive' directory
(full of files that you intend to store long term
and spend disk space backing up),
but somewhere else on your hard drive.
We'll call it 'the incoming folder'.
PictureArchiver allows you to browse through the incoming pictures,
rotate ones that need rotating, compress ones that are unnecessarily large,
and hit 'a' to import 'the good ones' into your archive folder.

Original files are always kept around in '.originals' folders.

Rotation and flipping is lossless and requires JPEGTran.
"jpegtran" should be on your command path.

Compression requires GraphicsMagick.
"gm" should be on your command path.

## Dependencies

Java, of course.

Image rotation and flipping require ~jpegtran~ to be on the system command path.

Compression uses GraphicsMagick.  ~gm~ should be somewhere on the system command-path for that to work.

### Easy Installation on Windows

The easiest way to get them installed on Windows is to run ~util/install-tools.bat~.
This will install Chocolatey, openjdk, jpegtran, and imagemagick.
If Chocolatey was not already installed you may need to restart your shell or manually add its ~bin~ dir to ~%PATH%~.

Otherwise, do some variation of the following:

```bat
choco install openjdk jpegtran imagemagick
```

## TODO

See [TODO.org](TODO.org) for...desired improvements to this program.
