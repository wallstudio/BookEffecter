import sys
import numpy as np
import cv2
import pdb
import pylab as plt
import random
import os 

class LearndImage:
    
    _akaze = None
    
    def __init__(self, path='??', image=None, akaze=None):
        self.imagePath = path
        try:self.imageName = os.path.basename(self.imagePath)
        except:self.imageName = '?'
        if akaze is None:
            LearndImage._akaze = cv2.AKAZE_create()
        else:
            LearndImage._akaze = akaze
        if image is None:
            self.image = cv2.imread(self.imagePath)
            self.image = cv2.cv2.cvtColor(self.image, cv2.COLOR_BGR2GRAY)
        else:
            self.image = image    
        self.keyPoints, self.descriptions = LearndImage._akaze.detectAndCompute(self.image, None)
                    
class LearndImageSet:

    def __init__(self, directoryPath:str):
        self.path = directoryPath
        self.imageNames = os.listdir(directoryPath)
        self.imagePaths = map(lambda x:directoryPath+x, self.imageNames)
        self.akaze = cv2.AKAZE_create()
        self.bfMatcher = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)
        self._calc()
    
    def _calc(self):
        self.learndImages = []
        for imagePath in self.imagePaths:
            learndImage = LearndImage(path=imagePath, akaze=self.akaze)
            self.learndImages.append(learndImage)
            
    def search(self, inputImagePath='', inputImage=None, drawResult=False) -> (LearndImage, float, list, np.ndarray):
        if inputImage is None:
            learndInputImage = LearndImage(path=inputImagePath,akaze=self.akaze)
        else:
            learndInputImage = LearndImage(image=inputImage,akaze=self.akaze)
        kepoint_i = learndInputImage.keyPoints
        descrip_i = learndInputImage.descriptions
        bestScore = 1000000.0
        bestMatches = None
        best = None
        scores = []
        for learndImage in self.learndImages:
            kepoint_d = learndImage.keyPoints
            descrip_d = learndImage.descriptions
            matches = self.bfMatcher.match(descrip_i,descrip_d)
            matches = sorted(matches, key = lambda x:x.distance)
            distancies = [m.distance for m in matches]
            score = sum(distancies)/len(distancies)
            scores.append(score)
            if  score < bestScore:
                bestScore = score
                best = learndImage
                bestMatches = matches

        if drawResult:
            resultImage = self._resultShow(best, learndInputImage, bestMatches, bestScore)
            return (best, bestScore, scores, resultImage)        

        return (best, bestScore, scores, None)

    def _resultShow(self, dataImage:LearndImage, inputImage:LearndImage, matches, score:float) -> np.ndarray:
        resultImage = cv2.drawMatches(inputImage.image, inputImage.keyPoints, dataImage.image, dataImage.keyPoints, matches[:10], None,flags=2)
        h,w,_ = resultImage.shape
        if score < 120: textColor = (255,255,255)
        else: textColor = (50,50,255)
        resultImage = cv2.putText(resultImage, str(int(score)), (0,h-20), cv2.FONT_HERSHEY_DUPLEX, 1,textColor, thickness=2)
        return resultImage

class CorrectedImage:
    def __init__(self, image:np.ndarray, vanisingRate:(float,float), pageAreaRatio:float, name="???", drawProcessing=False, size=256):
        self.name = name
        self.resultImage =  np.copy(image)
        self.resultImage = cv2.cvtColor(self.resultImage,cv2.COLOR_BGR2GRAY)
        h, w = self.resultImage.shape
        self.vanising = (int(vanisingRate[0]*w), int(vanisingRate[1]*h))
        self.pageAreaY = int(pageAreaRatio*h)
        self.cross0 = (int((1-((self.pageAreaY-self.vanising[1])/(h-self.vanising[1])))*self.vanising[0]), self.pageAreaY)
        self.cross1 = (w-int((1-((self.pageAreaY-self.vanising[1])/(h-self.vanising[1])))*self.vanising[0]), self.pageAreaY)

        self.size = size
        self.pts1 = np.float32([self.cross0,self.cross1,(0,h),(w,h)])
        self.pts2 = np.float32([[0,0],[size,0],[0,size],[size,size]])
        self.PersMatrix = cv2.getPerspectiveTransform(self.pts1,self.pts2)
        self.resultImage = cv2.warpPerspective(self.resultImage,self.PersMatrix,(size,size))
        self.resultImage = cv2.flip(self.resultImage,-1)
        
        if drawProcessing:
            color = (255,100,0,100)
            self.processingImage = np.copy(image)
            self.processingImage = cv2.line(self.processingImage, self.vanising, (0,h), color, thickness=3)
            self.processingImage = cv2.line(self.processingImage, self.vanising, (w,h), color, thickness=3)
            self.processingImage = cv2.line(self.processingImage, self.cross0, self.cross1, color, thickness=3)
            self.processingImage = cv2.circle(self.processingImage,self.cross0,5,color,3)
            self.processingImage = cv2.circle(self.processingImage,self.cross1,5,color,3)

def resize(image:np.ndarray, ratio:float) -> np.ndarray:
    if len(image.shape) == 2:
        h, w = image.shape
    else:
        h, w, _ = image.shape
    return cv2.resize(image,(int(w*ratio),int(h*ratio)))

def main():
    set = 0
    # 正解データセットの学習
    learndImageSet = LearndImageSet('testdata/'+str(set)+'/origin/')
    # カメラ画像の読み込み
    dir = 'testdata/'+str(set)+'/in/'
    fs = os.listdir(dir)
    pages = range(0,len(fs))
    for page in pages:
        src = cv2.imread(dir+fs[page])
        src = cv2.resize(src,(1280, 720))
        # 射影補正
        vanisingPoint = (0.5,0.2)
        pageAreaRatio = 0.65
        correctedImage = CorrectedImage(src, vanisingPoint, pageAreaRatio, drawProcessing=True)
        # 照合
        best, bestScore, scores, resultImage = learndImageSet.search(inputImage=correctedImage.resultImage, drawResult=True)
        # データの表示
        windowID = str(random.random())
        cv2.imshow("input "+ windowID, resize(correctedImage.processingImage,0.25))
        cv2.imshow("result "+ windowID, resize(resultImage,0.5))
        print(','.join([str(int(x)) for x in scores]) + '\n')
    # 終了
    cv2.waitKey(0)
    cv2.destroyAllWindows()

main()