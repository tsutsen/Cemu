#pragma once
#include "config/CemuConfig.h"

struct gameProfileIntegerOption_t
{
	bool isPresent = false;
	sint32 value;
};

class GameProfile
{
	friend class GameProfileWindow;

public:
	static const uint32 kThreadQuantumDefault = 45000;

	bool Load(uint64_t title_id);
	void Save(uint64_t title_id);
	void ResetOptional();
	void Reset();

	[[nodiscard]] uint64 GetTitleId() const { return m_title_id; }
	[[nodiscard]] bool IsLoaded() const { return m_is_loaded; }
	[[nodiscard]] bool IsDefaultProfile() const { return m_is_default; }
	[[nodiscard]] const std::optional<std::string>& GetGameName() const { return m_gameName; }

	[[nodiscard]] const std::optional<bool>& ShouldLoadSharedLibraries() const { return m_loadSharedLibraries; }
	void SetShouldLoadSharedLibraries(bool shouldLoadSharedLibraries) { m_loadSharedLibraries = shouldLoadSharedLibraries; }
	[[nodiscard]] bool StartWithGamepadView() const { return m_startWithPadView; }

	[[nodiscard]] const std::optional<GraphicAPI>& GetGraphicsAPI() const { return m_graphics_api; }
	[[nodiscard]] const AccurateShaderMulOption& GetAccurateShaderMul() const { return m_accurateShaderMul; }
	void SetAccurateShaderMul(AccurateShaderMulOption accurateShaderMulOption) { m_accurateShaderMul = accurateShaderMulOption; }
#if ENABLE_METAL
	[[nodiscard]] bool GetShaderFastMath() const { return m_shaderFastMath; }
	[[nodiscard]] MetalBufferCacheMode GetBufferCacheMode() const { return m_metalBufferCacheMode; }
	[[nodiscard]] PositionInvariance GetPositionInvariance() const { return m_positionInvariance; }
#endif
	[[nodiscard]] const std::optional<PrecompiledShaderOption>& GetPrecompiledShadersState() const { return m_precompiledShaders; }

	[[nodiscard]] uint32 GetThreadQuantum() const { return m_threadQuantum; }
	void SetThreadQuantum(uint32 threadQuantum){ m_threadQuantum = threadQuantum; }
	[[nodiscard]] const std::optional<CPUMode>& GetCPUMode() const { return m_cpuMode; }
	void SetCPUMode(CPUMode cpuMode) { m_cpuMode = cpuMode; }

	[[nodiscard]] bool IsAudioDisabled() const { return m_disableAudio; }

	[[nodiscard]] const std::array< std::optional<std::string>, 8>& GetControllerProfile() const { return m_controllerProfile; }

#if BOOST_PLAT_ANDROID
  public:
	struct DriverSetting
	{
		DriverSettingMode mode = DriverSettingMode::Global;
		std::optional<std::string> customPath;
	};

	[[nodiscard]] DriverSetting GetDriverSetting() const
	{
		return m_driverSetting;
	}

	void SetDriverSetting(DriverSetting driverSetting)
	{
		m_driverSetting = driverSetting;
	}

#endif

  public:
	// Bar overlay settings (Android) - top and bottom bars only (Wii U TV is 16:9, displayed on 4:3 screen)
	[[nodiscard]] bool GetBarOverlayEnabled() const { return m_barOverlayEnabled; }
	void SetBarOverlayEnabled(bool enabled) { m_barOverlayEnabled = enabled; }

	[[nodiscard]] const std::optional<std::string>& GetTopBarImagePath() const { return m_topBarImagePath; }
	void SetTopBarImagePath(const std::optional<std::string>& path) { m_topBarImagePath = path; }

	[[nodiscard]] const std::optional<std::string>& GetBottomBarImagePath() const { return m_bottomBarImagePath; }
	void SetBottomBarImagePath(const std::optional<std::string>& path) { m_bottomBarImagePath = path; }

	[[nodiscard]] float GetTopBarAlpha() const { return m_topBarAlpha; }
	void SetTopBarAlpha(float alpha) { m_topBarAlpha = alpha; }

	[[nodiscard]] float GetBottomBarAlpha() const { return m_bottomBarAlpha; }
	void SetBottomBarAlpha(float alpha) { m_bottomBarAlpha = alpha; }

  private:
	uint64_t m_title_id = 0;
	bool m_is_loaded = false;
	bool m_is_default = true;

	std::optional<std::string> m_gameName{};

	// general settings
	std::optional<bool> m_loadSharedLibraries{}; // = true;
	bool m_startWithPadView = false;

	// graphic settings
	std::optional<GraphicAPI> m_graphics_api{};
	AccurateShaderMulOption m_accurateShaderMul = AccurateShaderMulOption::True;
#if ENABLE_METAL
	bool m_shaderFastMath = true;
	MetalBufferCacheMode m_metalBufferCacheMode = MetalBufferCacheMode::Auto;
	PositionInvariance m_positionInvariance = PositionInvariance::Auto;
#endif
	std::optional<PrecompiledShaderOption> m_precompiledShaders{};
	// cpu settings
	uint32 m_threadQuantum = kThreadQuantumDefault; // values: 20000 45000 60000 80000 100000
	std::optional<CPUMode> m_cpuMode{}; // = CPUModeOption::kSingleCoreRecompiler;
	// audio
	bool m_disableAudio = false;
	// controller settings
	std::array< std::optional<std::string>, 8> m_controllerProfile{};

#if BOOST_PLAT_ANDROID
	DriverSetting m_driverSetting;

	// bar overlay settings (top and bottom bars only - Wii U TV is 16:9, displayed on 4:3 screen)
	bool m_barOverlayEnabled = false;
	std::optional<std::string> m_topBarImagePath{};
	std::optional<std::string> m_bottomBarImagePath{};
	float m_topBarAlpha = 1.0f;
	float m_bottomBarAlpha = 1.0f;
#endif
};
extern std::unique_ptr<GameProfile> g_current_game_profile;

void gameProfile_load();
