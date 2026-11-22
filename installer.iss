[Setup]
AppName=GoCrawl Desktop
AppVersion=1.0.0
AppPublisher=GoCrawl
DefaultDirName={localappdata}\Programs\GoCrawl
DefaultGroupName=GoCrawl Desktop
PrivilegesRequired=lowest
OutputDir=dist
OutputBaseFilename=GoCrawl-Setup-1.0.0
Compression=lzma2
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "dist\GoCrawl\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\GoCrawl Desktop"; Filename: "{app}\GoCrawl.exe"; WorkingDir: "{app}"
Name: "{group}\Uninstall GoCrawl Desktop"; Filename: "{uninstallexe}"
Name: "{autodesktop}\GoCrawl Desktop"; Filename: "{app}\GoCrawl.exe"; WorkingDir: "{app}"; Tasks: desktopicon

[Run]
Filename: "{app}\GoCrawl.exe"; WorkingDir: "{app}"; Description: "{cm:LaunchProgram,GoCrawl Desktop}"; Flags: nowait postinstall skipifsilent
