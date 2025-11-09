; =============================================================================
; IMPORTANT: This application requires Java Runtime Environment (JRE) or
;            Java Development Kit (JDK) version 24 or later.
;
; If Java 24 is not installed, the application will not run.
; Download from: https://www.oracle.com/java/technologies/downloads/
;                or use OpenJDK: https://adoptium.net/
; =============================================================================

#define AppName="SortaBeat";
#define AppVer="1.0.0";

#define AppJar="SortaBeat-1.0.0.jar"

#define Publisher="Neural Cortex";

[Setup]
AppName={#AppName}
AppVersion={#AppVer}
AppPublisher={#Publisher}
DefaultDirName={localappdata}\{#Publisher}\{#AppName}
DefaultGroupName={#Publisher}\{#AppName}
Compression=bzip
SolidCompression=true
OutputDir=.
UsePreviousAppDir=false
OutputBaseFilename={#AppName} {#AppVer} Setup
PrivilegesRequired=lowest
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64

[Tasks]
Name: desktopicon; Description: {cm:CreateDesktopIcon}; GroupDescription: {cm:AdditionalIcons}; Flags: unchecked

[Files]
Source: start.bat; DestDir: {app}; Flags: ignoreversion
Source: target\*.jar; DestDir: {app}; Flags: ignoreversion
Source: target\lib\*.jar; DestDir: {app}\lib; Flags: ignoreversion
Source: config\*.xml; DestDir: {app}\config; Flags: ignoreversion
Source: *.ico; DestDir: {app}; Flags: ignoreversion

[Icons]
Name: {group}\{#AppName}; Filename: {app}\start.bat; WorkingDir: {app} ; Flags:runminimized;IconFilename: {app}\logo.ico
Name: {group}\Uninstall; Filename: {uninstallexe}; WorkingDir: {app}; Flags:runminimized; IconFilename: {app}\unistall.ico
Name: {userdesktop}\{#AppName}; Filename: {app}\start.bat; Tasks: desktopicon; Flags:runminimized;IconFilename: {app}\logo.ico

[UninstallDelete]
Type: filesandordirs; Name: {app}

[Languages]
Name: Englisch; MessagesFile: compiler:Default.isl

[Messages]
SetupWindowTitle=%1 Setup - Java 24 Required

[CustomMessages]
JavaRequired=Warning: This application requires Java 24 or later to run.%n%nPlease install Java before continuing.%n%nDownload:%n• Oracle JDK/JRE: https://www.oracle.com/java/%n• Eclipse Temurin (OpenJDK): https://adoptium.net/

[Code]
function IsJava24Installed(): Boolean;
var
  JavaPath: String;
  Version: String;
  ResultCode: Integer;
begin
  Result := False;
  if RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment\24', 'JavaHome', JavaPath) or
     RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\JDK\24', 'JavaHome', JavaPath) then
  begin
    if Exec('cmd.exe', '/c "' + JavaPath + '\bin\java.exe" -version 2>&1', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
      Result := True;
  end;
end;

function InitializeSetup(): Boolean;
begin
  if not IsJava24Installed() then
  begin
    MsgBox(CustomMessage('JavaRequired'), mbCriticalError, MB_OK);
    // Optionally abort: Result := False;
  end;
  Result := True;
end;