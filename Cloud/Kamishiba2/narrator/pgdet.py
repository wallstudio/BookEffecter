import sys
import numpy as np
import cv2
import pylab as plt
import random
import os 
import datetime
from scipy.spatial import Delaunay
import traceback

# グローバル
PACKAGES = {} # package_name: TrainingDataList

# 定数
X = 0
Y = 1
BLUE = (255, 0, 0)
PROCESSING_SIZE = (480, 640)
USE_KEYPOINTS_COUNT = 10
NEED_KEYPOINTS_COUNT = USE_KEYPOINTS_COUNT

# 共通オブジェクト
AKAZE = cv2.AKAZE_create()
BF_MATCHER = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)

class NotEnoughKeypointsError(Exception):
    pass

class FailedImageDecodeError(Exception):
    pass

class FailedDelaunyError(Exception):
    pass

class Edge():
    """((x, y), (x,y))"""
    def __init__(self, i0:int, i1:int):
        self.index = (i0, i1)

    def __eq__(self, other):
        return sorted(self.index) == sorted(other.index)

    def __hash__(self):
        return hash(tuple(sorted(self.index)))

    def __str(self):
        return str(self.index)
        
    def lookup_geo(self, keypoints_mat:np.ndarray, out_type=float):
        p0 = keypoints_mat[self.index[0]]
        p1 = keypoints_mat[self.index[1]]
        if out_type == int:
            return ((int(p0[X]), int(p0[Y])), (int(p1[X]), int(p1[Y])))
        elif out_type == float:
            return (tuple(p0), tuple(p1))

    def cross_check(self, pair, keypoints_mat, play_ratio) -> bool:
        e0 = self.lookup_geo(keypoints_mat)
        e1 = pair.lookup_geo(keypoints_mat)

        if e0 == e1: 
            return False

        ax = e0[0][X]
        ay = e0[0][Y]
        bx = e0[1][X]
        by = e0[1][Y]
        cx = e1[0][X]
        cy = e1[0][Y]
        dx = e1[1][X]
        dy = e1[1][Y]

        ta = (cx - dx) * (ay - cy) + (cy - dy) * (cx - ax)
        tb = (cx - dx) * (by - cy) + (cy - dy) * (cx - bx)
        tc = (ax - bx) * (cy - ay) + (ay - by) * (ax - cx)
        td = (ax - bx) * (dy - ay) + (ay - by) * (ax - dx)

        return tc * td < 0 and ta * tb < 0

class FeaturedImage():
    """特徴情報と画像ndarray"""
    def __init__(self, path_or_image):
        try:
            if isinstance(path_or_image, str):
                self.image = cv2.imread(path_or_image)
            elif isinstance(path_or_image, np.ndarray):
                self.image = path_or_image
        except Exception as ex:
            raise FailedImageDecodeError('画像の読み込みエラー\n' + traceback.format_exc())
        # サイズの正規化
        h, w, _ = self.image.shape
        if h > w:
            self.image = cv2.resize(self.image, PROCESSING_SIZE, interpolation=cv2.INTER_CUBIC)
        else:
            self.image = cv2.resize(self.image, tuple(reversed(PROCESSING_SIZE)), interpolation=cv2.INTER_CUBIC)
        # AKAZE
        self.keypoints, self.descriptors = AKAZE.detectAndCompute(self.image, None)
        if len(self.keypoints) < NEED_KEYPOINTS_COUNT or len(self.descriptors) < NEED_KEYPOINTS_COUNT:
            raise NotEnoughKeypointsError('Keypointが不足 kp:{0}, dc: {1}, need:{2}'.format(len(self.keypoints), len(self.descriptors), NEED_KEYPOINTS_COUNT))
        self.keypoints_mat = self._convert_keypoint_to_mat(self.keypoints)
        self.edges = set()

    def create_delauny_edges(self, filter_matches:[cv2.DMatch]=None):
        self.edges.clear()
        try:
            if filter_matches != None:
                filter_index = sorted([m.queryIdx for m in filter_matches])
                use_keypoints = [kp for i, kp in zip(range(len(self.keypoints)), self.keypoints_mat) if i in filter_index]
                index_triangles = Delaunay(use_keypoints).simplices
                for index in index_triangles:
                    self.edges.add(Edge(filter_index[index[0]], filter_index[index[1]]))
                    self.edges.add(Edge(filter_index[index[1]], filter_index[index[2]]))
                    self.edges.add(Edge(filter_index[index[2]], filter_index[index[0]]))
            else:
                index_triangles = Delaunay(self.keypoints_mat).simplices
                for index in index_triangles:
                    self.edges.add(Edge(index[0], index[1]))
                    self.edges.add(Edge(index[1], index[2]))
                    self.edges.add(Edge(index[2], index[0]))
        except Exception as ex:
            raise FailedDelaunyError('デロニー処理内のエラー\n' + traceback.format_exc())

    def recontruct_edges(self, base_featued_image, matches:[cv2.DMatch]):
        self.edges.clear()
        for base_edge in base_featued_image.edges:
            index0 = [m.trainIdx for m in matches if m.queryIdx == base_edge.index[0]][0]
            index1 = [m.trainIdx for m in matches if m.queryIdx == base_edge.index[1]][0]
            self.edges.add(Edge(index0, index1))

    def get_cross(self):
        cross = 0
        for e0 in self.edges:
            for e1 in self.edges:
                if e0.cross_check(e1, self.keypoints_mat, 0.1):
                    cross = cross + 1
        return cross

    def draw_keypoint(self, override=False) -> np.ndarray:
        return cv2.drawKeypoints(self.image, self.keypoints, self.image if override else None)

    def draw_edges(self, override=False) -> np.ndarray:
        if override:
            image = self.image
        else:
            image = self.image.copy()

        for edge in self.edges:
            geo = edge.lookup_geo(self.keypoints_mat, out_type=int)
            cv2.line(image, geo[0], geo[1], BLUE, thickness=2)

        return image

    def draw_matches(self, train_image, matches:cv2.DMatch) -> np.ndarray:
        
        return cv2.drawMatches(self.image, self.keypoints, train_image.image, train_image.keypoints, matches, None, flags=2) # 2:NOT_DRAW_SINGLE_POINTS 

    # private
    def _convert_keypoint_to_mat(self, keypoints: [cv2.KeyPoint]) -> np.ndarray:
        buffer = []
        for keypoint in keypoints:
            x, y = keypoint.pt
            buffer.append([x, y])
        return np.array(buffer)

class TrainingDataList(list):
    def __init__(self, dir_path: str):
        super().__init__()
        files = os.listdir(dir_path)
        for file in sorted(files):
            _, extention = os.path.splitext(file)
            if extention in ['.jpg', '.jpeg', '.png'] and not 'min' in file:
                self.append(FeaturedImage(os.path.join(dir_path, file)))
        
        if len(self) <= 0:
            raise IOError('データベースがありません\n' + traceback.format_exc())

    def find(self, input_image) -> (int, [cv2.DMatch], [float]):
        if isinstance(input_image, str):
            input_image = FeaturedImage(input_image)

        scores = []
        best_score = 1000000.0
        best = -1

        for i, train in zip(range(len(self)), self):
            matches = BF_MATCHER.match(input_image.descriptors, train.descriptors)
            matches = sorted(matches, key=lambda x: x.distance)[:10]
            distancies = [m.distance for m in matches]
            score = sum(distancies)/len(distancies)
            scores.append(score)
            if  score < best_score:
                best_score = score
                best = i
                best_matches = matches
        return best, best_matches, scores

    def draw_match(self, input_image: FeaturedImage, number: int, matches: [cv2.DMatch]) -> np.ndarray:
        return cv2.drawMatches(
            input_image.image, input_image.keypoints,
            self[number].image, self[number].keypoints,
            matches, None, flags=2)

def im_write_show(path:str, title:str, image:np.ndarray):
    if not os.path.exists(os.path.dirname(path)):
        os.makedirs(os.path.dirname(path))
    cv2.imwrite(path + ".png", image)
    cv2.imshow(title, image)

def write_print(path:str, text:str):
    if not os.path.exists(os.path.dirname(path)):
        os.makedirs(os.path.dirname(path))
    with open(path + '.log', mode='a') as file:
        file.write(text + '\r\n')
    print(text)

def main(args):
    TIME = str(datetime.datetime.now()).replace(':', '_')
    PREFIX = os.path.join(r'C:\Users\huser\Desktop\result', args)
    TRAINING_DATAS = TrainingDataList(r'C:\Users\huser\Desktop\yukamaki\yukawallstudio.exam')
    INPUT_IMAGE = FeaturedImage(r"C:\Users\huser\Desktop\in\{0}.jpg".format(args))

    im_write_show(PREFIX + '_1', '1) Detect Keypoint by AKAZE.', INPUT_IMAGE.draw_keypoint(override=True))
    idx, matches, score = TRAINING_DATAS.find(INPUT_IMAGE)
    im_write_show(PREFIX + '_0', '0) -', TRAINING_DATAS[idx].draw_keypoint(override=True))
    im_write_show(PREFIX + '_2', '2) Matching calcurate. Distance: {0}'.format(score[idx]), 
            INPUT_IMAGE.draw_matches(TRAINING_DATAS[idx], matches))
    INPUT_IMAGE.create_delauny_edges(matches)
    im_write_show(PREFIX + '_3', '3) Make mesh by Delauny method on input. Cross: {0}'.format(INPUT_IMAGE.get_cross()), INPUT_IMAGE.draw_edges(override=True))
    TRAINING_DATAS[idx].recontruct_edges(INPUT_IMAGE, matches)
    TRAINING_DATAS[idx].draw_keypoint(override=True)
    im_write_show(PREFIX + '_4', '4) Make mesh by Matching relations on train. Cross: {0}'.format(TRAINING_DATAS[idx].get_cross()), TRAINING_DATAS[idx].draw_edges(override=True))
    im_write_show(PREFIX + '_5', '5) -', 
            INPUT_IMAGE.draw_matches(TRAINING_DATAS[idx], matches))

    write_print(PREFIX, 'Distance score: ' + str(score))
    write_print(PREFIX, 'Distance top score: ' + str(score[idx]))
    write_print(PREFIX, 'Cross score (input): ' + str(INPUT_IMAGE.get_cross()))
    write_print(PREFIX, 'Cross score (train): ' + str(TRAINING_DATAS[idx].get_cross()))

if __name__ == '__main__':
    for no in range(6):
        print(no)
        main(str(no))
    #cv2.waitKey(0)
    cv2.destroyAllWindows()
else:
    print('Initialize start page detector')
