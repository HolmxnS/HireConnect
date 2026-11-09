"""Explicit test-stage configuration. Set ML_MODEL_MODE=real to leave demo mode."""
import os

DEFAULT_MODEL_MODE = 'demo'


def model_mode(environment=None):
    env = os.environ if environment is None else environment
    value = env.get('ML_MODEL_MODE', DEFAULT_MODEL_MODE).lower()
    if value not in {'demo', 'real'}:
        raise ValueError('ML_MODEL_MODE debe ser demo o real')
    return value


if __name__ == '__main__':
    print(model_mode())
