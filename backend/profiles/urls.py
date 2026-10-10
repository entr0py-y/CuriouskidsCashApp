from django.urls import path
from profiles.views import (
    BranchListView, BranchDetailView, ProfileView, CompletionView,
    PhotoUploadUrlView, FamilyMemberListCreateView, FamilyMemberDetailView,
    LegalDocumentsView, RecordConsentView, DataExportView
)

urlpatterns = [
    # Branches
    path('branches', BranchListView.as_view(), name='branch-list'),
    path('branches/<uuid:pk>', BranchDetailView.as_view(), name='branch-detail'),

    # Profile & Completion
    path('me/profile', ProfileView.as_view(), name='my-profile'),
    path('me/completion', CompletionView.as_view(), name='my-completion'),
    path('me/profile/photo-upload-url', PhotoUploadUrlView.as_view(), name='photo-upload-url'),
    path('me/data-export', DataExportView.as_view(), name='data-export'),

    # Family Members
    path('me/family', FamilyMemberListCreateView.as_view(), name='family-list-create'),
    path('me/family/<uuid:pk>', FamilyMemberDetailView.as_view(), name='family-detail'),

    # Legal & Consents
    path('legal/documents', LegalDocumentsView.as_view(), name='legal-documents'),
    path('me/consents', RecordConsentView.as_view(), name='record-consent'),
]
