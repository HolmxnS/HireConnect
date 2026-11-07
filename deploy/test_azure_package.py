from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile
from package_azure import package, verify_package

class PackageTests(unittest.TestCase):
    def source(self, path):
        root = path / 'source'
        (root / 'ml').mkdir(parents=True)
        (root / 'deploy').mkdir()
        for name in ['app.py', 'train.py', 'requirements.lock.txt']:
            (root / 'ml' / name).write_text('fixture')
        for name in ['start.py', 'azure-start.sh']:
            (root / 'deploy' / name).write_text('fixture')
        (root / '.env').write_text('private-root-value')
        (root / 'ml/.env').write_text('private-model-value')
        (root / '.git').mkdir()
        (root / '.git/config').write_text('private-history')
        (root / 'ml/artifacts').mkdir()
        (root / 'ml/artifacts/model.joblib').write_text('local-model')
        runtime = path / 'python'
        (runtime / 'bin').mkdir(parents=True)
        (runtime / 'bin/python3').write_text('python')
        (runtime / 'bin/python3').chmod(0o755)
        (runtime / 'lib').mkdir()
        (runtime / 'lib/runtime.py').write_text('runtime')
        jar = path / 'application.jar'
        with ZipFile(jar, 'w') as archive:
            archive.writestr('META-INF/MANIFEST.MF', 'Main-Class: example.Application')
        return root, runtime, jar

    def test_includes_both_services_but_never_environment_or_history(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            root, runtime, jar = self.source(path)
            output = path / 'app.zip'
            report = package(root, runtime, jar, output)
            verify_package(output)
            with ZipFile(output) as archive:
                names = set(archive.namelist())
                self.assertIn('app.jar', names)
                self.assertIn('.python/bin/python3', names)
                self.assertIn('deploy/azure-start.sh', names)
                self.assertFalse(any('.env' in name or '.git/' in name or 'artifacts/' in name for name in names))
                self.assertTrue((archive.getinfo('.python/bin/python3').external_attr >> 16) & 0o100)
            self.assertEqual(len(report['sha256']), 64)

    def test_refuses_a_jar_only_package(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'jar-only.zip'
            with ZipFile(output, 'w') as archive:
                archive.writestr('app.jar', 'jar')
            with self.assertRaisesRegex(ValueError, 'Python'):
                verify_package(output)

    def test_runtime_cannot_follow_a_link_to_private_files(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            root, runtime, jar = self.source(path)
            (runtime / 'lib/private').symlink_to(root / '.env')
            with self.assertRaisesRegex(ValueError, 'enlaces'):
                package(root, runtime, jar, path / 'app.zip')

if __name__ == '__main__':
    unittest.main()
