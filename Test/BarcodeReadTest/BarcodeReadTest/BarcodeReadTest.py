import sys
import numpy as np
import cv2
import pdb
import pylab as plt
import random
import os 

global set

set = 2

def pmatch(pat):
    result1 = []
    result2 = []
    max = 100000
    maxIdx = -1
    dir = 'testdata/'+str(set)+'/origin/'
    fs = os.listdir(dir)
    for i in range(0,len(fs)):
        path = dir + fs[i]
        origin = cv2.imread(path)
        origin = cv2.cvtColor(origin, cv2.COLOR_BGR2GRAY)
        #origin = cv2.blur(origin, (30,30))
        #res = cv2.matchTemplate(origin,pat,cv2.TM_CCOEFF)
        #min_val, max_val, min_loc, max_loc = cv2.minMaxLoc(res)

        akaze = cv2.AKAZE_create()
        kp1, des1 = akaze.detectAndCompute(origin, None)
        kp2, des2 = akaze.detectAndCompute(pat, None)
        bf = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)
        matches = bf.match(des1,des2)
        matches = sorted(matches, key = lambda x:x.distance)
        dist = [m.distance for m in matches]
        img3 = cv2.drawMatches(origin,kp1,pat,kp2,matches[:10], None,flags=2)
        #good = []
        #for m, n in matches:
        #    if m.distance < ratio * n.distance:
        #        good.append([m])
        #img3 = cv2.drawMatchesKnn(origin, kp1, pat, kp2, good, None,flags=2)
        h, w, c = img3.shape
        img3 = cv2.resize(img3, (w//2, h//2))
        score = sum(dist)/len(dist)
        img3 = cv2.putText(img3,str(score),(w//2-100,h//2-100),cv2.FONT_HERSHEY_COMPLEX,1,(255,255,0))
        if  score < max:
            max = score
            maxIdx = i

        #cv2.imshow('Kanaze ' + str(i), img3)
        result1.append(img3)
        result2.append(str(int(score)))

    print(','.join(result2) + '\n')
    return (maxIdx, result1)


vanising = 0.2
dir = 'testdata/'+str(set)+'/in/'
fs = os.listdir(dir)
pages = range(0,len(fs))
for page in pages:
    src = cv2.imread(dir+fs[page])
    src = cv2.resize(src,(1280, 720))
    h,w,ch = src.shape
    vanP = (w//2, int(h*vanising))

    pro0 = np.copy(src)
    pro0 = cv2.line(pro0, vanP, (0,h), (255,100,0,100), thickness=3)
    pro0 = cv2.line(pro0, vanP, (w,h), (255,100,0,100), thickness=3)

    gray = cv2.cvtColor(src,cv2.COLOR_BGR2GRAY)
    edges = cv2.Canny(gray,50,150,apertureSize = 3)
    #lines = cv2.HoughLines(edges,1,np.pi/180,200)
    #edgeY = h
    #for l in lines:
    #    for rho,theta in l:
    #        a = np.cos(theta)
    #        b = np.sin(theta)
    #        if a > 0.1 or -0.1 > a:
    #            continue 
    #        x0 = a*rho
    #        y0 = b*rho
    #        x1 = int(x0 + 1000*(-b))
    #        y1 = int(y0 + 1000*(a))
    #        x2 = int(x0 - 1000*(-b))
    #        y2 = int(y0 - 1000*(a))
    #        yy = max(y1,y2)
    #        if edgeY > yy:
    #            edgeY = yy
    #        cv2.line(pro0,(x1,y1),(x2,y2),(0,0,255),2)
    edgeY = int(h * 0.65)
    cross0 = (int((1-((edgeY-vanP[1])/(h-vanP[1])))*vanP[0]), edgeY)
    cross1 = (w-int((1-((edgeY-vanP[1])/(h-vanP[1])))*vanP[0]), edgeY)
    pro0 = cv2.circle(pro0,cross0,2,(0,255,0))
    pro0 = cv2.circle(pro0,cross1,2,(0,255,0))
    pro0 = cv2.resize(pro0,(w//2,h//2))
    #cv2.imshow('input '+ str(random.random()), pro0)

    size = 256
    pts1 = np.float32([cross0,cross1,(0,h),(w,h)])
    pts2 = np.float32([[0,0],[size,0],[0,size],[size,size]])
    M = cv2.getPerspectiveTransform(pts1,pts2)
    pattern = cv2.warpPerspective(src,M,(size,size))
    pattern = cv2.flip(pattern,-1)
    pattern = cv2.cvtColor(pattern,cv2.COLOR_BGR2GRAY)
    g = 0.4
    imax = pattern.max()
    #pattern = np.array(imax * (pattern / imax)**(1/g), 'uint8')
    #pattern = cv2.blur(pattern,(30,30))
    j, res1 = pmatch(pattern)

    cv2.imshow("result "+ str(random.random()), res1[j])

cv2.waitKey(0)
cv2.destroyAllWindows()
