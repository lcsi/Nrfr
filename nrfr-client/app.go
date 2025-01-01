package main

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/electricbubble/gadb"
	"github.com/wailsapp/wails/v2/pkg/runtime"
)

// App struct
type App struct {
	ctx            context.Context
	adbClient      gadb.Client
	selectedDevice *gadb.Device
}

// DeviceInfo 设备信息结构
type DeviceInfo struct {
	Serial  string `json:"serial"`
	State   string `json:"state"`
	Product string `json:"product"`
	Model   string `json:"model"`
}

// NewApp creates a new App application struct
func NewApp() *App {
	return &App{}
}

// startup is called when the app starts. The context is saved
func (a *App) startup(ctx context.Context) {
	a.ctx = ctx
	// 初始化 ADB 客户端
	client, err := gadb.NewClient()
	if err != nil {
		runtime.LogError(ctx, fmt.Sprintf("初始化ADB失败: %v", err))
		return
	}
	a.adbClient = client
}

// GetDevices 获取已连接的设备列表
func (a *App) GetDevices() []DeviceInfo {
	devices, err := a.adbClient.DeviceList()
	if err != nil {
		runtime.LogError(a.ctx, fmt.Sprintf("获取设备列表失败: %v", err))
		return nil
	}

	var deviceInfos []DeviceInfo
	for _, device := range devices {
		state, _ := device.State()
		product, _ := device.Product()
		model, _ := device.Model()

		info := DeviceInfo{
			Serial:  device.Serial(),
			State:   string(state),
			Product: product,
			Model:   model,
		}
		deviceInfos = append(deviceInfos, info)
	}
	return deviceInfos
}

// SelectDevice 选择设备
func (a *App) SelectDevice(serial string) error {
	devices, err := a.adbClient.DeviceList()
	if err != nil {
		return fmt.Errorf("获取设备列表失败: %v", err)
	}

	for _, device := range devices {
		if device.Serial() == serial {
			a.selectedDevice = &device
			return nil
		}
	}
	return fmt.Errorf("未找到设备: %s", serial)
}

// CheckApps 检查必要的应用是否已安装
func (a *App) CheckApps() map[string]bool {
	if a.selectedDevice == nil {
		return nil
	}

	packages := map[string]string{
		"shizuku": "moe.shizuku.privileged.api",
		"nrfr":    "com.github.nrfr",
	}

	result := make(map[string]bool)
	for name, pkg := range packages {
		installed, _ := a.isPackageInstalled(pkg)
		result[name] = installed
	}
	return result
}

// isPackageInstalled 检查包是否已安装
func (a *App) isPackageInstalled(packageName string) (bool, error) {
	output, err := a.selectedDevice.RunShellCommand("pm", "list", "packages", packageName)
	if err != nil {
		return false, err
	}
	return strings.Contains(output, packageName), nil
}

// InstallShizuku 安装 Shizuku
func (a *App) InstallShizuku() error {
	if a.selectedDevice == nil {
		return fmt.Errorf("未选择设备")
	}

	// 从本地资源目录推送 APK 到设备
	execPath, err := os.Executable()
	if err != nil {
		return fmt.Errorf("获取执行路径失败: %v", err)
	}
	execDir := filepath.Dir(execPath)
	localApk := filepath.Join(execDir, "resources", "shizuku.apk")
	remoteApk := "/data/local/tmp/shizuku.apk"

	// 检查文件是否存在
	if _, err := os.Stat(localApk); os.IsNotExist(err) {
		return fmt.Errorf("shizuku apk 文件不存在: %s", localApk)
	}

	// 推送 APK 文件
	file, err := os.Open(localApk)
	if err != nil {
		return fmt.Errorf("打开 Shizuku APK 文件失败: %v", err)
	}
	defer file.Close()

	err = a.selectedDevice.Push(file, remoteApk, time.Now())
	if err != nil {
		return fmt.Errorf("推送 Shizuku APK 文件失败: %v", err)
	}

	// 安装 APK
	_, err = a.selectedDevice.RunShellCommand("pm", "install", "-r", remoteApk)
	if err != nil {
		return fmt.Errorf("安装 Shizuku 失败: %v", err)
	}

	// 清理临时文件
	_, err = a.selectedDevice.RunShellCommand("rm", remoteApk)
	if err != nil {
		runtime.LogWarning(a.ctx, fmt.Sprintf("清理临时文件失败: %v", err))
	}

	return nil
}

// InstallNrfr 安装 Nrfr
func (a *App) InstallNrfr() error {
	if a.selectedDevice == nil {
		return fmt.Errorf("未选择设备")
	}

	// 从本地资源目录推送 APK 到设备
	execPath, err := os.Executable()
	if err != nil {
		return fmt.Errorf("获取执行路径失败: %v", err)
	}
	execDir := filepath.Dir(execPath)
	localApk := filepath.Join(execDir, "resources", "nrfr.apk")
	remoteApk := "/data/local/tmp/nrfr.apk"

	// 检查文件是否存在
	if _, err := os.Stat(localApk); os.IsNotExist(err) {
		return fmt.Errorf("nrfr apk 文件不存在: %s", localApk)
	}

	// 推送 APK 文件
	file, err := os.Open(localApk)
	if err != nil {
		return fmt.Errorf("打开 Nrfr APK 文件失败: %v", err)
	}
	defer file.Close()

	err = a.selectedDevice.Push(file, remoteApk, time.Now())
	if err != nil {
		return fmt.Errorf("推送 Nrfr APK 文件失败: %v", err)
	}

	// 安装 APK
	_, err = a.selectedDevice.RunShellCommand("pm", "install", "-r", remoteApk)
	if err != nil {
		return fmt.Errorf("安装 Nrfr 失败: %v", err)
	}

	// 清理临时文件
	_, err = a.selectedDevice.RunShellCommand("rm", remoteApk)
	if err != nil {
		runtime.LogWarning(a.ctx, fmt.Sprintf("清理临时文件失败: %v", err))
	}

	return nil
}

// StartShizuku 启动 Shizuku
func (a *App) StartShizuku() error {
	if a.selectedDevice == nil {
		return fmt.Errorf("未选择设备")
	}

	// 先启动 Shizuku 应用
	_, err := a.selectedDevice.RunShellCommand("monkey", "-p", "moe.shizuku.privileged.api", "1")
	if err != nil {
		return fmt.Errorf("启动 shizuku 应用失败: %v", err)
	}

	// 等待应用启动
	time.Sleep(time.Second * 2)

	// 执行启动脚本
	output, err := a.selectedDevice.RunShellCommand("sh", "/sdcard/Android/data/moe.shizuku.privileged.api/start.sh")
	if err != nil {
		return fmt.Errorf("启动 shizuku 服务失败: %v", err)
	}
	runtime.LogInfo(a.ctx, fmt.Sprintf("shizuku 启动输出: %s", output))
	return nil
}

// WindowMinimise 最小化窗口
func (a *App) WindowMinimise() {
	runtime.WindowMinimise(a.ctx)
}

// WindowMaximise 最大化窗口
func (a *App) WindowMaximise() {
	runtime.WindowToggleMaximise(a.ctx)
}

// WindowClose 关闭窗口
func (a *App) WindowClose() {
	runtime.Quit(a.ctx)
}

// StartNrfr 启动 Nrfr 应用
func (a *App) StartNrfr() error {
	if a.selectedDevice == nil {
		return fmt.Errorf("no device selected")
	}

	// 使用 monkey 启动 Nrfr
	_, err := a.selectedDevice.RunShellCommand("monkey", "-p", "com.github.nrfr", "1")
	if err != nil {
		return fmt.Errorf("failed to start nrfr: %v", err)
	}

	return nil
}
