# PictureArchiver 4.1

A utility to help import ('archive') pictures from.

Idea is you dump all your incoming JPEGs into a structure
that mirrors that of an 'archive' directory
(full of files that you intend to store long term
and spend disk space backing up),
but somewhere else on your hard drive.
We'll call it 'the incoming folder'.
PictureArchiver allows you to browse through the incoming pictures,
rotate ones that need rotating, compress ones that are unnecessarily large,
and hit 'A' to import 'the good ones' into your archive folder.

Original files are always kept around in '.originals' folders.

Rotation and flipping is lossless and requires JPEGTran.
"jpegtran" should be on your command path.

Compression requires GraphicsMagick.
"gm" should be on your command path.

TODO: Write about -archive-map, etc.
