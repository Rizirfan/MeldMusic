const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');

// Request single instance lock to prevent cache collisions and database lock errors
const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  app.quit();
} else {
  let mainWindow = null;

  function createWindow() {
    mainWindow = new BrowserWindow({
      width: 1280,
      height: 830,
      minWidth: 1000,
      minHeight: 700,
      icon: path.join(__dirname, 'icon.png'),
      webPreferences: {
        nodeIntegration: true,
        contextIsolation: false
      },
      // Frameless window with custom title bar for premium look
      frame: false,
      transparent: true,
      backgroundColor: '#00000000'
    });

    mainWindow.loadFile('index.html');

    mainWindow.on('closed', () => {
      mainWindow = null;
    });
  }

  // Window IPC Controls
  ipcMain.on('window-minimize', () => {
    if (mainWindow) {
      mainWindow.minimize();
    }
  });

  ipcMain.on('window-maximize', () => {
    if (mainWindow) {
      if (mainWindow.isMaximized()) {
        mainWindow.unmaximize();
      } else {
        mainWindow.maximize();
      }
    }
  });

  ipcMain.on('window-close', () => {
    if (mainWindow) {
      mainWindow.close();
    }
  });

  app.on('second-instance', () => {
    // Someone tried to run a second instance, focus the existing window.
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.focus();
    }
  });

  app.whenReady().then(() => {
    createWindow();

    app.on('activate', () => {
      if (BrowserWindow.getAllWindows().length === 0) {
        createWindow();
      }
    });
  });

  app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
      app.quit();
    }
  });
}
