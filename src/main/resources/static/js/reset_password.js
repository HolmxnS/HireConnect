function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    const button = input.parentElement.querySelector('.toggle-password');
    const icon = button.querySelector('i');

    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
    }
}

document.getElementById('resetForm').addEventListener('submit', function (e) {
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (password !== confirmPassword) {
        e.preventDefault();
        alert('Las contraseñas no coinciden');
    }
});

document.getElementById('password').addEventListener('input', function (e) {
    const password = e.target.value;
    const strengthBar = document.getElementById('strengthBar');
    const strengthText = document.getElementById('strengthText');
    const strengthContainer = document.getElementById('passwordStrength');

    if (password.length === 0) {
        strengthContainer.style.display = 'none';
        return;
    }

    strengthContainer.style.display = 'block';

    let strength = 0;
    if (password.length >= 6) strength++;
    if (password.length >= 8) strength++;
    if (password.match(/[a-z]+/)) strength++;
    if (password.match(/[A-Z]+/)) strength++;
    if (password.match(/[0-9]+/)) strength++;

    const percentage = (strength / 5) * 100;
    strengthBar.style.width = percentage + '%';

    if (strength <= 1) {
        strengthBar.style.backgroundColor = '#ff4444';
        strengthText.textContent = 'Muy débil';
        strengthText.style.color = '#ff4444';
    } else if (strength === 2) {
        strengthBar.style.backgroundColor = '#ff8800';
        strengthText.textContent = 'Débil';
        strengthText.style.color = '#ff8800';
    } else if (strength === 3) {
        strengthBar.style.backgroundColor = '#ffaa00';
        strengthText.textContent = 'Aceptable';
        strengthText.style.color = '#ffaa00';
    } else if (strength === 4) {
        strengthBar.style.backgroundColor = '#88cc00';
        strengthText.textContent = 'Fuerte';
        strengthText.style.color = '#88cc00';
    } else {
        strengthBar.style.backgroundColor = '#00cc66';
        strengthText.textContent = 'Muy fuerte';
        strengthText.style.color = '#00cc66';
    }
});