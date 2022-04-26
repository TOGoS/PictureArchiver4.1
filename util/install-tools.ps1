# Run in powershell like this:
# Set-ExecutionPolicy Bypass -Scope Process -Force ; .\choco-install-synthgen-dev-tools.ps1

$install_extras = $false
$force = $false

for ( $i = 0; $i -lt $args.count; $i++ ) {
	if( $args[$i] -eq "-InstallExtras" ) {
		$install_extras = $true
	} elseif( $args[$i] -eq "-Force" ) {
		$force = $true
	} else {
		Write-Error "Invalid argument: $($args[$i])"
		Exit 1
	}
}

if (-not $force) {
	$current_principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
	if (-not $current_principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
		Write-Error (@(
			"Not Administrator!"
			"Try running this script as administrator,"
			"or run with -Force to skip this check."
		) -join "`r`n")
		Exit 1
	}
}

Set-ExecutionPolicy Bypass -Scope Process -Force

if (-not (Get-Command "choco.exe" -ErrorAction SilentlyContinue)) {
	Write-Output "choco not installed??  Let's make it be!"
	[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
	iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
} else {
	Write-Output "# choco already installed, w00t"
}


# We might, of course, need java.

choco install -y openjdk jpegtran imagemagick

# We can even install slack!
if ($install_extras) {
	Write-Output "# Installing extra stuff!"
	Write-Output "# ...actually just kidding, there isn't any."
}
