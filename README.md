# exploratorium
Collaborative project with Exploratorium on real-time identifying and tracking fish samples under microscope

### fishTracker.m
New code to mark fish eggs with red circles
* findEggs.m: output the location and size of the fish eggs in an given image
* checkForBubble.m: remove false positives (air bubbles) from the result
* sacleImage.m: reduce the size of input image to accelarate computation
