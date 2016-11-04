# -*- coding: utf-8 -*-
"""
Python code to detect fish eggs with OpenCV circular Hough transform function.
V1.0 Nov 4th, 2016
@Author: Bing Dai (bdai6@wisc.edu)
"""

import cv2
import argparse
import numpy as np

# construct the argument parser and parse the arguments
#ap = argparse.ArgumentParser()
#ap.add_argument("-i", "--image", required = True, help = "Path to the image")
#args = vars(ap.parse_args())
#
## load the image, clone it for output, and then convert it to grayscale
#img = cv2.imread(args["image"])
#output = img.copy()
#gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# import image
#img = cv2.imread('../images/junkfinder_25x_dic_frame1.jpg')  # 3 eggs
#img = cv2.imread('../images/Kip2201.jpg')  # 1.5 eggs
img = cv2.imread('../images/bubble3.jpg')  # air bubbles
#output = img.copy()
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)  # convert to grayscale
#img = cv2.medianBlur(img,5)
#cv2.imshow('original image',img)

# shrink image size to make the computation faster
# Calculate the ratio of the new image to the old image
# make sure the aspect ratio doesn't change
ratio = 200.0 / img.shape[1]
dim = (200, int(img.shape[0] * ratio))
# perform the actual resizing of the image
resized = cv2.resize(gray, dim, interpolation = cv2.INTER_AREA)
#cv2.imshow("resized", resized)

# Detect circles (fish eggs) in the image
#circles = cv2.HoughCircles(resized,cv2.HOUGH_GRADIENT,1,50,
#                            param1=50,param2=30,minRadius=10,maxRadius=30)
circles = cv2.HoughCircles(resized,cv2.HOUGH_GRADIENT,1,50,
                            param1=20,param2=30,minRadius=35,maxRadius=50)

# ensure at least some circles were found
if circles is not None:
    # convert the (x, y) coordinates and radius of the circles to integers
    circles = np.uint16(np.around(circles))
    
    for i in circles[0,:]:
        # convert center and radius back to original image scale
        (x,y,r)= np.uint16(np.around(np.dot(i,1/ratio)))
        # draw the outer circle on original image
        print 'Center:',x,y,'Radius:',r
        cv2.circle(img,(x,y),r,(0,255,128),2)
        # draw the center of the circle
        cv2.circle(img,(x,y),2,(0,128,255),3)

    cv2.imshow('detected fish eggs',img)
#    cv2.imshow('detected fish eggs',np.hstack([img, output]))
    cv2.waitKey(0)
    cv2.destroyAllWindows()
else:
    print 'No Eggs Found'