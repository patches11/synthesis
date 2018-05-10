# SYNTHESIS #

### What is this repository for? ###

* Synthesis E11 2017

### How do I get set up? ###

* To do

### Post Illuminate

* Loaded string should update in background

### For Illuminate

* L takeover, Static
* Pippa Video
* Fix Face detection & Make it look better
* Sparse Triangle Mode
* Brightness

### To-Do

* Write mixer mode to put face on top of background - Better
* Save faces and merge/play back
* Person recognition - Flash unique pattern
* Spotlight
* Silhouette
  * Forest
  * City
* Pixel art
* Trippy Trees [https://trasevol-dog.itch.io/forest](https://trasevol-dog.itch.io/forest)
* Geometric rainbow out
* Follow people's head
* More dark designs
* Fluid / Smoke Simulation
* Glitch
* Pippa Video
* Flash Pippa?
* Rain
* Hacking
* Scrolling Text

### For Burning Man 2017

* Rain
* Glitch
* Pippa Video
* Kinect
    * Follow people's head
    * Painting
* Person recognition - Flash unique pattern
* Fix Face Detection and Display
* More Dark Designs
* Fix Blink

### AI

* ???


### Bugs

* Face issue


## Skeltrack


## Ubuntu

### libfreenect

`git clone https://github.com/OpenKinect/libfreenect`

Install udev rules

`sudo apt-get install git cmake build-essential libusb-1.0-0-dev`

`cd libfreenect/`

`mkdir build`

`cd build`

`cmake -L ..`

`make`

`sudo make install`

### GFreenect

`git clone https://github.com/elima/GFreenect.git`

`cd GFreenect/`

`sudo apt-get install gtk-doc-tools autoconf libgirepository1.0-dev gobject-introspection`

`./autogen.sh`

`make`

`sudo make install`


### Skeltrack

`sudo apt-get install clutter-1.0`

`https://github.com/joaquimrocha/Skeltrack.git`

`cd Skeltrack`

`vim tests/Makefile.am`

Add on line 16:

`AM_LDFLAGS = -lgio-2.0 -lgobject-2.0 -lglib-2.0`

Then

`./autogen.sh --enable-examples=yes`

`make`

`sudo make install`

## Mac OS X - Not Working

#### GFreenect

`brew install libusb`

`brew install libfreenect`

`brew install gtk-doc`

`brew install cairo`

`git clone https://github.com/elima/GFreenect.git`

`./autogen.sh --enable-gtk-doc`

`./configure`

`make`

`sudo make install`

#### Skeltrack

`./autogen --enable-examples=yes`

`make`
`make install`
 
If you have trouble with the place where gfreenect.h is, try setting the following environment variables:
 
`export LD_LIBRARY_PATH=/usr/lib64`

`export PKG_CONFIG_PATH=/usr/lib64/pkgconfig`
