from rest_framework.pagination import CursorPagination

class SkyjumpersCursorPagination(CursorPagination):
    ordering = '-created_at'
    page_size = 50
