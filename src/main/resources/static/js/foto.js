document.addEventListener('DOMContentLoaded', function () {
    const profileBanner = document.getElementById('profileAvatarClickArea');
    const fileInput = document.getElementById('fileInput');
    const fotoUploadForm = document.getElementById('fotoUploadForm');
    profileBanner.addEventListener('click', function () {
        fileInput.click();
    });

    fileInput.addEventListener('change', function () {
        if (this.files && this.files.length > 0) {
            fotoUploadForm.submit();
        }
    });
});