const form = document.getElementById('register-form');
const step1 = document.getElementById('step-1');
const step2 = document.getElementById('step-2');
const stepInput = document.getElementById('currentStep');
const headerStep = document.querySelector('.header__step');

const mostrarPaso2 = window.mostrarPaso2 || false;


if (mostrarPaso2) {
    step1.style.display = 'none';
    step2.style.display = 'block';
    step2.style.transition = 'opacity 0.45s ease, transform 0.45s ease';
    headerStep.textContent = 'Paso 2 de 3';
    requestAnimationFrame(() => {
        step2.style.opacity = '1';
        step2.style.transform = 'translateX(0)';
    });
    stepInput.value = '2';
}

form.addEventListener('submit', function () {
    if (stepInput.value === '1') {
        step1.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        step1.style.opacity = '0';
        step1.style.transform = 'translateX(-40px)';
    }

    if (stepInput.value === '2') {
        step2.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        step2.style.opacity = '0';
        step2.style.transform = 'translateX(-40px)';
    }
});

function togglePassword() {
    const input = document.getElementById('password');
    const icon = document.getElementById('eye-icon');
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.replace('fa-eye', 'fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.replace('fa-eye-slash', 'fa-eye');
    }
}

function toggleConfirmPassword() {
    const input = document.getElementById('confirmPassword');
    const icon = document.getElementById('eye-icon-confirm');
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.replace('fa-eye', 'fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.replace('fa-eye-slash', 'fa-eye');
    }
}

document.getElementById('confirmPassword').addEventListener('input', function () {
    const password = document.getElementById('password').value;
    const confirm = this.value;
    const errorEl = this.closest('.form-group').querySelector('.error-match');

    if (!errorEl) {
        const p = document.createElement('p');
        p.className = 'error error-match';
        this.closest('.input-password-wrapper').insertAdjacentElement('afterend', p);
    }

    const msg = this.closest('.form-group').querySelector('.error-match');
    if (confirm && password !== confirm) {
        msg.textContent = 'Las contraseñas no coinciden';
    } else {
        msg.textContent = '';
    }
});