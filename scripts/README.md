# Matlab and OpenCV-Python code for detecting fish eggs in an image
## Instruction on configuring OpenCV-Python package for Windows
Credit to Yili Zhao (panovr@gmail.com)

1. Install Visual Studio 2015 community edition - https://www.visualstudio.com/downloads/#d-community
2. Install Anaconda 4.2.0 Pyton 2.7 - https://www.continuum.io/downloads
3. Download `OpenCV 2.4.13` or `OpenCV 3.1.0` (preferred) - https://sourceforge.net/projects/opencvlibrary/files/opencv-win/
4. Copy the `opencv\build\python\...\cv2.pyd` to the Anaconda Python site-packages directory
5. Add OpenCV bin directory (such as `opencv\build\x64\vc14\bin`) to Windows PATH environment 
6. You can just use the pre-compiled DLLs from the OpenCV package as well, so you shouldn't need to compile OpenCV with CMake by youself

## Contributors
* Guneet S Mehta (gmehta2@wisc.edu)
* Bing Dai (bdai6@wisc.edu)
