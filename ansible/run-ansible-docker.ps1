param(
    [string]$SshKeyPath = "C:\Users\Utilizador\Downloads\week6-key.pem"
)

$ansibleDir = Split-Path -Parent $MyInvocation.MyCommand.Path

docker run --rm `
  -v "${ansibleDir}:/work" `
  -v "${SshKeyPath}:/tmp/week6-key.pem:ro" `
  -w /work `
  cytopia/ansible:latest `
  ansible-playbook -i inventory.ini site.yml
