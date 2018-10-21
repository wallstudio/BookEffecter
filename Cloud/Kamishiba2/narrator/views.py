from django.http import HttpResponse, HttpResponse, Http404, JsonResponse, HttpResponseBadRequest
from django.shortcuts import render
from . import pgdet
from django.views.decorators.csrf import csrf_exempt
from PIL import Image
import numpy as np
import cv2
import os
import uuid
import sys
import traceback
from datetime import datetime
from django.views.generic import TemplateView
import json
from Kamishiba2 import local_secret

def succes_log(*messages):
    with open('succes.log', mode='a', encoding='utf-8') as log:
        log.write(datetime.now().strftime('%Y/%m/%d %H:%M:%S') + '\n')
        for message in messages:
            log.write(str(message) + '\n')
    print(message)

def error_log(*messages):
    with open('error.log', mode='a', encoding='utf-8') as log:
        log.write(datetime.now().strftime('%Y/%m/%d %H:%M:%S') + '\n')
        for message in messages:
            log.write(str(message) + '\n')
    print(message)

def get_hash() -> str:
    value = str(uuid.uuid4())
    value = value[0:4]
    return value

# XHRで呼ばれるAPI
# Ex. /narrator/api (POST:package,image)
@csrf_exempt
def api(request):
    request_hash = get_hash()

    try:
        # Ex. 'kamishiba_ws.yukamaki'
        package_name = request.POST['package']
        # 送られてきた画像（カメラ）
        image = np.array(Image.open(request.FILES['image'])) 
    except:
        message = 'パラメータ/画像が読み込めません ' + request_hash
        error_log(request_hash, message, traceback.format_exc())
        return HttpResponseBadRequest(message)

    try:
        if not package_name in pgdet.PACKAGES:
            package_dir = os.path.join(local_secret.PACKAGE_DIR, package_name)
            pgdet.PACKAGES[package_name] = pgdet.TrainingDataList(package_dir)
        package = pgdet.PACKAGES[package_name]
    except:
        message = 'データベースがありません/読み込めません "{0}" '.format(package_name) + request_hash
        error_log(request_hash, message, traceback.format_exc())
        raise Http404(message)

    try:
        featured_image = pgdet.FeaturedImage(image)
        idx, matches, score = package.find(featured_image)
        featured_image.create_delauny_edges(matches)
        package[idx].recontruct_edges(featured_image, matches)
    except:
        message = '検索結果がありません/検索ができません ' + request_hash
        error_log(request_hash, message, traceback.format_exc())
        raise Http404(message)

    try:
        packages_dir = os.path.join(os.path.dirname(__file__), 'static', 'narrator', 'packages')
        with open(os.path.join(packages_dir, package_name, 'timing.json')) as f:
            timing = json.loads(f.read())
    except Exception as ex:
        message = 'サーバーの内部エラー ' + request_hash
        error_log(request_hash, message, traceback.format_exc())
        return HttpResponseBadRequest(message)

    retval = {'id': request_hash, 'index': idx, 'score': score[idx], 'cross': package[idx].get_cross(), 'timing': timing}
    succes_log(request_hash, str(retval))
    return JsonResponse(retval)

def package_list(request):
    request_hash = get_hash()
    try:
        package_index_list = []
        packages_dir = os.path.join(os.path.dirname(__file__), 'static', 'narrator', 'packages')
        dirs_or_files = os.listdir(packages_dir)
        for package_name in dirs_or_files:
            if os.path.isdir(os.path.join(packages_dir, package_name)):
                with open(os.path.join(packages_dir, package_name, 'title'), encoding='utf-8') as f:
                    title = f.readline().strip()
                    package_index_list.append({'package_id': package_name, 'package_title': title})
    except Exception as ex:
        message = 'サーバーの内部エラー ' + request_hash
        error_log(request_hash, message, traceback.format_exc())
        return HttpResponseBadRequest(message)

    retval = {'id': request_hash, 'packages': package_index_list} 
    succes_log(request_hash, str(retval))
    return JsonResponse(retval)

class AppView(TemplateView):
    template_name = 'narrator/app.html'