from django.http import HttpResponse, HttpResponse, Http404, JsonResponse
from django.shortcuts import render
from . import pgdet
from django.views.decorators.csrf import csrf_exempt
from PIL import Image
import numpy as np
import cv2
import os

PACKAGES_DIR = r'C:\Users\huser\Desktop\yukamaki'

@csrf_exempt
def index(request):
    # Ex. 'kamishiba_ws.yukamaki'
    package_name = request.POST['package']
    if not package_name in pgdet.PACKAGES:
        package_dir = os.path.join(PACKAGES_DIR, package_name)
        pgdet.PACKAGES[package_name] = pgdet.TrainingDataList(package_dir)
    package = pgdet.PACKAGES[package_name]
    
    # 送られてきた画像（カメラ）
    image = np.array(Image.open(request.FILES['image'])) 
    featured_image = pgdet.FeaturedImage(image)
    idx, matches, score = package.find(featured_image)
    featured_image.create_delauny_edges(matches)
    package[idx].recontruct_edges(featured_image, matches)

    # デバッグ用に表示
    #featured_image.draw_keypoint(override=True)
    #featured_image.draw_edges(override=True)
    #package[idx].draw_keypoint(override=True)
    #package[idx].draw_edges(override=True)
    #debug_image = featured_image.draw_matches(package[idx], matches)
    #title = 'deb idx:{0} score:{1} cross:{2}'.format(idx, score[idx], package[idx].get_cross())
    #cv2.imshow(title, debug_image)
    #cv2.waitKey(0)
    #cv2.destroyAllWindows()

    return JsonResponse({'index': idx, 'socre': score[idx], 'cross': package[idx].get_cross()})
