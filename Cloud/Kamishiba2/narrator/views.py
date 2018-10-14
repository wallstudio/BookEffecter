from django.http import HttpResponse, HttpResponse, Http404
from django.shortcuts import render
from . import pgdetct

def index(request):
    return HttpResponse("Maki")
    