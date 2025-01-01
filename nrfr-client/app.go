package main

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
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

// AppStatus 应用状态结构
type AppStatus struct {
	Shizuku bool `json:"shizuku"`
	Nrfr    struct {
		Installed  bool `json:"installed"`
		NeedUpdate bool `json:"needUpdate"`
	} `json:"nrfr"`
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
func (a *App) CheckApps() AppStatus {
	if a.selectedDevice == nil {
		return AppStatus{
			Shizuku: false,
			Nrfr: struct {
				Installed  bool `json:"installed"`
				NeedUpdate bool `json:"needUpdate"`
			}{
				Installed:  false,
				NeedUpdate: false,
			},
		}
	}

	// 检查 Shizuku
	shizukuInstalled, _ := a.isPackageInstalled("moe.shizuku.privileged.api")

	// 检查 Nrfr
	nrfrInstalled, _ := a.isPackageInstalled("com.github.nrfr")
	needUpdate := false
	if nrfrInstalled {
		needUpdate, _ = a.CheckNrfrUpdate()
	}

	return AppStatus{
		Shizuku: shizukuInstalled,
		Nrfr: struct {
			Installed  bool `json:"installed"`
			NeedUpdate bool `json:"needUpdate"`
		}{
			Installed:  nrfrInstalled,
			NeedUpdate: needUpdate,
		},
	}
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

// GetAppVersion 获取已安装应用的版本号
func (a *App) GetAppVersion(packageName string) (string, error) {
	if a.selectedDevice == nil {
		return "", fmt.Errorf("未选择设备")
	}

	output, err := a.selectedDevice.RunShellCommand("dumpsys", "package", packageName, "|", "grep", "versionName")
	if err != nil {
		return "", fmt.Errorf("获取版本号失败: %v", err)
	}

	// 解析版本号
	parts := strings.Split(strings.TrimSpace(output), "=")
	if len(parts) != 2 {
		return "", fmt.Errorf("解析版本号失败")
	}
	return strings.TrimSpace(parts[1]), nil
}

// compareVersions 比较两个版本号，如果 v1 < v2 返回 -1，v1 = v2 返回 0，v1 > v2 返回 1
func compareVersions(v1, v2 string) int {
	// 移除可能的 'v' 前缀
	v1 = strings.TrimPrefix(v1, "v")
	v2 = strings.TrimPrefix(v2, "v")

	// 分割版本号
	parts1 := strings.Split(v1, ".")
	parts2 := strings.Split(v2, ".")

	// 确保两个版本号都有三个部分
	for len(parts1) < 3 {
		parts1 = append(parts1, "0")
	}
	for len(parts2) < 3 {
		parts2 = append(parts2, "0")
	}

	// 比较每个部分
	for i := 0; i < 3; i++ {
		num1, _ := strconv.Atoi(parts1[i])
		num2, _ := strconv.Atoi(parts2[i])

		if num1 < num2 {
			return -1
		}
		if num1 > num2 {
			return 1
		}
	}

	return 0
}

// CheckNrfrUpdate 检查Nrfr是否需要更新
func (a *App) CheckNrfrUpdate() (bool, error) {
	if a.selectedDevice == nil {
		return false, fmt.Errorf("未选择设备")
	}

	// 检查是否已安装
	installed, err := a.isPackageInstalled("com.github.nrfr")
	if err != nil {
		return false, err
	}

	if !installed {
		return true, nil // 未安装，需要安装
	}

	// 获取已安装版本
	currentVersion, err := a.GetAppVersion("com.github.nrfr")
	if err != nil {
		return false, err
	}

	// 最新版本号（从build.gradle.kts中获取）
	latestVersion := "1.0.1" // 这里硬编码为当前最新版本

	// 比较版本号
	return compareVersions(currentVersion, latestVersion) < 0, nil
}
