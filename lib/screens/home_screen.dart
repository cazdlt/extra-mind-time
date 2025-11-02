import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'app_selection_screen.dart';
import 'settings_screen.dart';
import '../services/app_monitor_service.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<String> _selectedApps = [];
  Map<String, String> _appNames = {};
  int _delaySeconds = 5;
  String _mindfulMessage = "Take a moment to breathe and be present.";
  Color _backgroundColor = Colors.deepPurple;
  int _recheckIntervalMinutes = 30;
  bool _isMonitoring = false;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    setState(() => _isLoading = true);

    final prefs = await SharedPreferences.getInstance();

    final appsJson = prefs.getString('selected_apps');
    final apps = appsJson != null
        ? List<String>.from(json.decode(appsJson))
        : <String>[];

    final appNamesJson = prefs.getString('app_names');
    final appNames = appNamesJson != null
        ? Map<String, String>.from(json.decode(appNamesJson))
        : <String, String>{};

    final delay = prefs.getInt('delay_seconds') ?? 5;
    final message =
        prefs.getString('mindful_message') ??
        "Take a moment to breathe and be present.";
    final backgroundColorValue =
        prefs.getInt('background_color') ?? Colors.deepPurple.value;
    final recheckInterval = prefs.getInt('recheck_interval_minutes') ?? 30;
    final monitoring = prefs.getBool('is_monitoring') ?? false;

    setState(() {
      _selectedApps = apps;
      _appNames = appNames;
      _delaySeconds = delay;
      _mindfulMessage = message;
      _backgroundColor = Color(backgroundColorValue);
      _recheckIntervalMinutes = recheckInterval;
      _isMonitoring = monitoring;
      _isLoading = false;
    });

    if (_isMonitoring) {
      await AppMonitorService.startMonitoring();
    }
  }

  Future<void> _saveSettings() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('selected_apps', json.encode(_selectedApps));
    await prefs.setString('app_names', json.encode(_appNames));
    await prefs.setInt('delay_seconds', _delaySeconds);
    await prefs.setString('mindful_message', _mindfulMessage);
    await prefs.setInt('background_color', _backgroundColor.value);
    await prefs.setInt('recheck_interval_minutes', _recheckIntervalMinutes);
    await prefs.setBool('is_monitoring', _isMonitoring);
  }

  Future<void> _toggleMonitoring() async {
    setState(() => _isMonitoring = !_isMonitoring);
    await _saveSettings();

    if (_isMonitoring) {
      final success = await AppMonitorService.startMonitoring();
      if (!success && mounted) {
        setState(() => _isMonitoring = false);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Failed to start monitoring. Check permissions.'),
          ),
        );
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Monitoring started!'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } else {
      await AppMonitorService.stopMonitoring();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Monitoring stopped'),
            backgroundColor: Colors.orange,
          ),
        );
      }
    }
  }

  Future<void> _navigateToAppSelection() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AppSelectionScreen(
          selectedApps: _selectedApps,
          appNames: _appNames,
        ),
      ),
    );

    if (result != null && result is Map<String, dynamic>) {
      setState(() {
        _selectedApps = List<String>.from(result['selectedApps']);
        _appNames = Map<String, String>.from(result['appNames']);
      });
      await _saveSettings();
    }
  }

  Future<void> _navigateToSettings() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => SettingsScreen(
          delaySeconds: _delaySeconds,
          mindfulMessage: _mindfulMessage,
          backgroundColor: _backgroundColor,
          recheckIntervalMinutes: _recheckIntervalMinutes,
        ),
      ),
    );

    if (result != null && result is Map<String, dynamic>) {
      setState(() {
        _delaySeconds = result['delaySeconds'] as int;
        _mindfulMessage = result['mindfulMessage'] as String;
        _backgroundColor = result['backgroundColor'] as Color;
        _recheckIntervalMinutes = result['recheckIntervalMinutes'] as int;
      });
      await _saveSettings();
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Extra Mind Time'),
        centerTitle: true,
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: _navigateToSettings,
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Monitoring Status Card
            Card(
              color: _isMonitoring ? Colors.green[50] : Colors.grey[100],
              elevation: 4,
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  children: [
                    Icon(
                      _isMonitoring ? Icons.visibility : Icons.visibility_off,
                      size: 60,
                      color: _isMonitoring ? Colors.green : Colors.grey,
                    ),
                    const SizedBox(height: 16),
                    Text(
                      _isMonitoring
                          ? 'Monitoring Active'
                          : 'Monitoring Inactive',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: _isMonitoring
                            ? Colors.green[700]
                            : Colors.grey[700],
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _isMonitoring
                          ? 'Watching for selected apps'
                          : 'Tap the button below to start',
                      style: const TextStyle(fontSize: 14, color: Colors.grey),
                    ),
                    const SizedBox(height: 16),
                    ElevatedButton.icon(
                      onPressed: _selectedApps.isEmpty
                          ? null
                          : _toggleMonitoring,
                      icon: Icon(_isMonitoring ? Icons.stop : Icons.play_arrow),
                      label: Text(
                        _isMonitoring ? 'Stop Monitoring' : 'Start Monitoring',
                        style: const TextStyle(fontSize: 16),
                      ),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 24,
                          vertical: 12,
                        ),
                        backgroundColor: _isMonitoring
                            ? Colors.red
                            : Colors.green,
                        foregroundColor: Colors.white,
                      ),
                    ),
                    if (_selectedApps.isEmpty)
                      const Padding(
                        padding: EdgeInsets.only(top: 8.0),
                        child: Text(
                          'Select apps first to start monitoring',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.orange,
                            fontStyle: FontStyle.italic,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),

            // Selected Apps Card
            Card(
              elevation: 2,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.apps, color: Colors.deepPurple),
                        const SizedBox(width: 12),
                        const Text(
                          'Selected Apps',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const Spacer(),
                        TextButton.icon(
                          onPressed: _navigateToAppSelection,
                          icon: const Icon(Icons.edit, size: 18),
                          label: const Text('Edit'),
                        ),
                      ],
                    ),
                    const Divider(),
                    const SizedBox(height: 8),
                    _selectedApps.isEmpty
                        ? const Center(
                            child: Padding(
                              padding: EdgeInsets.all(16.0),
                              child: Column(
                                children: [
                                  Icon(
                                    Icons.info_outline,
                                    size: 48,
                                    color: Colors.grey,
                                  ),
                                  SizedBox(height: 8),
                                  Text(
                                    'No apps selected yet',
                                    style: TextStyle(
                                      fontSize: 16,
                                      color: Colors.grey,
                                    ),
                                  ),
                                  SizedBox(height: 8),
                                  Text(
                                    'Tap "Edit" to select apps to monitor',
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: Colors.grey,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          )
                        : Column(
                            children: _selectedApps
                                .map(
                                  (packageName) => ListTile(
                                    leading: _buildAppIcon(packageName),
                                    title: Text(
                                      _appNames[packageName] ?? packageName,
                                      style: const TextStyle(fontSize: 14),
                                    ),
                                    subtitle: Text(
                                      packageName,
                                      style: const TextStyle(
                                        fontSize: 11,
                                        color: Colors.grey,
                                      ),
                                    ),
                                    dense: true,
                                  ),
                                )
                                .toList(),
                          ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Current Settings Card
            Card(
              elevation: 2,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.tune, color: Colors.deepPurple),
                        SizedBox(width: 12),
                        Text(
                          'Current Settings',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    const Divider(),
                    const SizedBox(height: 8),
                    ListTile(
                      leading: const Icon(Icons.timer, color: Colors.blue),
                      title: const Text('Delay Duration'),
                      subtitle: Text('$_delaySeconds seconds'),
                      dense: true,
                    ),
                    ListTile(
                      leading: const Icon(Icons.message, color: Colors.purple),
                      title: const Text('Mindful Message'),
                      subtitle: Text(
                        _mindfulMessage,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      dense: true,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),

            // Info Card
            Card(
              color: Colors.blue[50],
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Row(
                  children: [
                    Icon(Icons.lightbulb, color: Colors.blue[700]),
                    const SizedBox(width: 12),
                    const Expanded(
                      child: Text(
                        'When you open a selected app, you\'ll see a mindful delay screen to help you be more intentional.',
                        style: TextStyle(fontSize: 13),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppIcon(String packageName) {
    // Try to get the base64 encoded icon data
    final iconData = _appNames['${packageName}_icon'];

    if (iconData != null && iconData.isNotEmpty) {
      try {
        final bytes = base64Decode(iconData);
        return Image.memory(
          bytes,
          width: 40,
          height: 40,
          errorBuilder: (context, error, stackTrace) {
            return const Icon(Icons.android, size: 40, color: Colors.green);
          },
        );
      } catch (e) {
        // Fallback to default icon if decoding fails
      }
    }

    // Fallback to default icon
    return const Icon(Icons.android, size: 40, color: Colors.green);
  }
}
