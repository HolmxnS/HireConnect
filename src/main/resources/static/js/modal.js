function closeModal() {
    const modal = document.getElementById('successModal');
    if (modal) {
        modal.style.display = 'none';
        modal.remove();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('successModal');
    
    if (modal) {
        const closeButton = modal.querySelector('.modal__close-btn');
        if (closeButton) {
            closeButton.addEventListener('click', closeModal);
        }
        modal.addEventListener('click', (e) => {
            if (e.target.id === 'successModal') {
                closeModal();
            }
        });
        modal.style.display = 'flex';
    }
});

function closeErrorModal() {
  const modal = document.getElementById('errorModal');
  if (modal) {
    modal.style.display = 'none';
    modal.remove();
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const errorModal = document.getElementById('errorModal');
  
  if (errorModal) {
    const closeButton = errorModal.querySelector('.modal__close-btn');
    if (closeButton) {
      closeButton.addEventListener('click', closeErrorModal);
    }
    errorModal.addEventListener('click', (e) => {
      if (e.target.id === 'errorModal') {
        closeErrorModal();
      }
    });
    errorModal.style.display = 'flex';
  }
});

function mostrarModalEliminar() {
    const modal = document.getElementById('deleteModal');
    if (modal) {
        modal.style.display = 'flex';
    }
}

function cerrarModalEliminar() {
    const modal = document.getElementById('deleteModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const deleteModal = document.getElementById('deleteModal');
    
    if (deleteModal) {
        deleteModal.addEventListener('click', (e) => {
            if (e.target.id === 'deleteModal') {
                cerrarModalEliminar();
            }
        });
    }
});