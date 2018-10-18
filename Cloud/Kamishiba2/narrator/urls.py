from django.urls import path
from . import views

urlpatterns = [
    path('/app', views.AppView.as_view(), name='app'),
    path('/package-list', views.package_list, name='package_list'),
    path('/api', views.api, name='api'),
]