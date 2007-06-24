# Auto-generated by EclipseNSIS Script Wizard
# 04.06.2007 12:43:30

Name Elexis

# Defines
!define PRODUCTNAME "Elexis"
!define VERSION 0.3.3

!define REGKEY "SOFTWARE\${PRODUCTNAME}"

!define COMPANY "G. Weirich"
!define URL http://www.elexis.ch
!define src "$%SOFTWARE%\elexis\deploy\${PRODUCTNAME}-${VERSION}"

# MUI defines
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_STARTMENUPAGE_REGISTRY_ROOT HKLM
!define MUI_STARTMENUPAGE_REGISTRY_KEY ${REGKEY}
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULTFOLDER Elexis
# !define MUI_FINISHPAGE_RUN test.cmd
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_LANGDLL_REGISTRY_ROOT HKLM
!define MUI_LANGDLL_REGISTRY_KEY ${REGKEY}
!define MUI_LANGDLL_REGISTRY_VALUENAME InstallerLanguage

# Included files
!include Sections.nsh
!include MUI.nsh

# Reserved Files
!insertmacro MUI_RESERVEFILE_LANGDLL

# Variables
Var StartMenuGroup

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE ..\liesmich.txt
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuGroup
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE German
!insertmacro MUI_LANGUAGE French
!insertmacro MUI_LANGUAGE Italian
!insertmacro MUI_LANGUAGE English

# Installer attributes
OutFile "$%SOFTWARE%\elexis\deploy\output\elexis-windows-${VERSION}.exe"
InstallDir $PROGRAMFILES\Elexis
CRCCheck on
XPStyle on
ShowInstDetails show
VIProductVersion 0.3.3.0
VIAddVersionKey /LANG=${LANG_GERMAN} ProductName Elexis
VIAddVersionKey ProductVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_GERMAN} CompanyName "${COMPANY}"
VIAddVersionKey /LANG=${LANG_GERMAN} CompanyWebsite "${URL}"
VIAddVersionKey /LANG=${LANG_GERMAN} FileVersion ""
VIAddVersionKey /LANG=${LANG_GERMAN} FileDescription ""
VIAddVersionKey /LANG=${LANG_GERMAN} LegalCopyright ""
InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails show

# Installer sections
Section -Main SEC0000
    SetOutPath $INSTDIR
    SetOverwrite on
    File /r "${src}\*.*"
    WriteRegStr HKLM "${REGKEY}\Components" Main 1

SectionEnd

Section -post SEC0001
    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    SetOutPath $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    ReadEnvStr $0 USERPROFILE
    CreateShortCut "$DESKTOP\Elexis.lnk" "$INSTDIR\Elexis.exe" '-data "$0\elexis"' "$INSTDIR\elexis.ico"
    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\Elexis.lnk" "$INSTDIR\Elexis.exe" '-data "$0\elexis"' "$INSTDIR\elexis.ico"
    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\Elexis Handbuch.lnk" "$INSTDIR\elexis.pdf"
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk" $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_END
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTNAME}" DisplayName "${PRODUCTNAME}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTNAME}" DisplayVersion "${VERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTNAME}" Publisher "${COMPANY}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTNAME}" URLInfoAbout "${URL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTNAME}" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTNAME}" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTNAME}" NoModify 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTNAME}" NoRepair 1
SectionEnd

# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    StrCmp $R0 1 0 next${UNSECTION_ID}
    !insertmacro SelectSection "${UNSECTION_ID}"
    GoTo done${UNSECTION_ID}
next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
    Pop $R0
!macroend

# Uninstaller sections
Section /o un.Main UNSEC0000
    RmDir /r /REBOOTOK $INSTDIR
    DeleteRegValue HKLM "${REGKEY}\Components" Main
SectionEnd

Section un.post UNSEC0001
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTNAME}"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    ;Delete /r "$SMPROGRAMS\$StartMenuGroup
    Delete "$DESKTOP\Elexis.lnk"
    Delete "$SMPROGRAMS\$StartMenuGroup\Elexis.lnk"
    Delete "$SMPROGRAMS\$StartMenuGroup\Elexis Handbuch.lnk"
    
    DeleteRegValue HKLM "${REGKEY}" StartMenuGroup
    DeleteRegValue HKLM "${REGKEY}" Path
    DeleteRegKey /IfEmpty HKLM "${REGKEY}\Components"
    DeleteRegKey /IfEmpty HKLM "${REGKEY}"
    RmDir /REBOOTOK $SMPROGRAMS\$StartMenuGroup
    RmDir /REBOOTOK $INSTDIR
    Push $R0
    StrCpy $R0 $StartMenuGroup 1
    StrCmp $R0 ">" no_smgroup
no_smgroup:
    Pop $R0
SectionEnd

# Installer functions
Function .onInit
    InitPluginsDir
    !insertmacro MUI_LANGDLL_DISPLAY
FunctionEnd


# Uninstaller functions
Function un.onInit
    ReadRegStr $INSTDIR HKLM "${REGKEY}" Path
    !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuGroup
    !insertmacro MUI_UNGETLANGUAGE
    !insertmacro SELECT_UNSECTION Main ${UNSEC0000}
FunctionEnd



# Installer Language Strings
# TODO Update the Language Strings with the appropriate translations.

LangString ^UninstallLink ${LANG_GERMAN} "Uninstall ${PRODUCTNAME}"
LangString ^UninstallLink ${LANG_FRENCH} "Uninstall ${PRODUCTNAME}"
LangString ^UninstallLink ${LANG_ITALIAN} "Uninstall ${PRODUCTNAME}"
LangString ^UninstallLink ${LANG_ENGLISH} "Uninstall ${PRODUCTNAME}"
