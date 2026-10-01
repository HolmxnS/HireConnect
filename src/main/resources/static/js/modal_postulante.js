let currentDeleteUrl = '';


function createAndShowModal(deleteUrl, itemType) {
  const modalHTML = `
        <div id="confirmModalDynamic" class="confirm-modal-style flex"> 
            <div class="modal-content">
                <h5>Confirmar Acción</h5>
                <p id="confirmMessageDynamic">¿Estás seguro de que quieres eliminar ${itemType}?</p>
                <div class="modal-actions">
                    <button id="cancelButtonDynamic" class="btn btn-cancel">Cancelar</button>
                    <button id="confirmButtonDynamic" class="btn btn-confirm">Confirmar</button>
                </div>
            </div>
        </div>
    `;
  document.body.insertAdjacentHTML('beforeend', modalHTML);
  const confirmModalDynamic = document.getElementById('confirmModalDynamic');
  currentDeleteUrl = deleteUrl;

  const cancelButton = document.getElementById('cancelButtonDynamic');
  const confirmButton = document.getElementById('confirmButtonDynamic');

  const closeModal = () => {
    if (confirmModalDynamic) {
      confirmModalDynamic.remove();
    }
  };

  cancelButton.onclick = closeModal;
  confirmButton.onclick = () => {
    if (currentDeleteUrl) window.location.href = currentDeleteUrl;
    closeModal();
  };

}

function showConfirmModal(button, itemType) {
  const deleteUrl = button.getAttribute('data-delete-url');
  createAndShowModal(deleteUrl, itemType);
}