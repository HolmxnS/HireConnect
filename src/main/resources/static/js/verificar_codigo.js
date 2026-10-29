document.addEventListener('DOMContentLoaded', function () {
    const inputs = Array.from(document.querySelectorAll('.code-input'));
    const btnVerify = document.getElementById('btnVerify');
    const codigoCompleto = document.getElementById('codigoCompleto');
    const btnResend = document.getElementById('btnResend');
    const timerText = document.getElementById('timerText');
    const countdown = document.getElementById('countdown');


    inputs.forEach((input, index) => {
        input.addEventListener('keydown', function (e) {
            const allowed = ['Backspace', 'Delete', 'Tab', 'ArrowLeft', 'ArrowRight'];
            if (!allowed.includes(e.key) && !/^[0-9]$/.test(e.key)) {
                e.preventDefault();
            }
        });

        input.addEventListener('input', function () {
            this.value = this.value.replace(/[^0-9]/g, '');

            if (this.value) {
                this.classList.add('filled');
                if (index < inputs.length - 1) {
                    inputs[index + 1].focus();
                }
            } else {
                this.classList.remove('filled');
            }

            actualizarCodigo();
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'Backspace' && !this.value && index > 0) {
                
                inputs[index - 1].focus();
                inputs[index - 1].value = '';
                inputs[index - 1].classList.remove('filled');
                actualizarCodigo();
            }
        });

        
        input.addEventListener('paste', function (e) {
            e.preventDefault();
            const pasted = (e.clipboardData || window.clipboardData)
                .getData('text')
                .replace(/[^0-9]/g, '')
                .slice(0, 6);

            pasted.split('').forEach((char, i) => {
                if (inputs[i]) {
                    inputs[i].value = char;
                    inputs[i].classList.add('filled');
                }
            });

           
            const lastFilled = Math.min(pasted.length, inputs.length) - 1;
            if (lastFilled >= 0) inputs[lastFilled].focus();

            actualizarCodigo();
        });
    });

    function actualizarCodigo() {
        const codigo = inputs.map(i => i.value).join('');
        codigoCompleto.value = codigo;

        
        btnVerify.disabled = codigo.length < 6;
    }

    
    inputs[0].focus();

    
    window.marcarError = function () {
        inputs.forEach(i => {
            i.classList.add('error');
            i.classList.remove('filled');
            setTimeout(() => i.classList.remove('error'), 500);
        });
        inputs.forEach(i => i.value = '');
        codigoCompleto.value = '';
        btnVerify.disabled = true;
        inputs[0].focus();
    };


    function iniciarTimer(segundos) {
        btnResend.disabled = true;
        timerText.style.display = 'block';
        let restante = segundos;
        countdown.textContent = restante;

        const interval = setInterval(() => {
            restante--;
            countdown.textContent = restante;
            if (restante <= 0) {
                clearInterval(interval);
                btnResend.disabled = false;
                timerText.style.display = 'none';
            }
        }, 1000);
    }

    
    iniciarTimer(60);

    
    btnResend.closest('form').addEventListener('submit', function () {
        iniciarTimer(60);
    });


    const modalOverlay = document.getElementById('modal-error');
    const btnModalClose = document.getElementById('btnModalClose');

    btnModalClose.addEventListener('click', function () {
        modalOverlay.classList.remove('active');
    });

    
    modalOverlay.addEventListener('click', function (e) {
        if (e.target === modalOverlay) modalOverlay.classList.remove('active');
    });

    const hayError = document.querySelector('.flash-alert--error');
    if (hayError) {
        window.marcarError();
        modalOverlay.classList.add('active');
    }
});