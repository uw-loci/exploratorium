function [centers,radii] = findEggs(frame,settings)
% To find fish eggs from a given frame;
% downsample the frame to less than imSize to reduce computation time;
% locate eggs given

reducedFrame = scaleImage(frame,settings.imSize);
imshow(reducedFrame,'Border','tight');
[centers, radii] = imfindcircles(reducedFrame,[settings.lowerRadius settings.higherRadius],'ObjectPolarity',settings.objectPolarity,'Sensitivity',settings.sensitivity);
% eliminat air bubbles from the resutls
[centers,radii]=checkForBubble(centers,radii,reducedFrame,settings.threshold);
viscircles(centers,radii);

end