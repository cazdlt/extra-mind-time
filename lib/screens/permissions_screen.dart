import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:usage_stats/usage_stats.dart';

class PermissionsScreen extends StatefulWidget {
  final VoidCallback onPermissionsGranted;

  const PermissionsScreen({super.key, required this.onPermissionsGranted});

  @override
  State<PermissionsScreen> createState() => _PermissionsScreenState();
}

class _PermissionsScreenState extends State<PermissionsScreen> {
  bool _usageStatsGranted = false;
  bool _overlayGranted = false;
  bool _notificationGranted = false;
  bool _isChecking = false;

  @override
  void initState() {
    super.initState();
    _checkAllPermissions();
  }

  Future<void> _checkAllPermissions() async {
    setState(() => _isChecking = true);

    final usageStats = await UsageStats.checkUsagePermission() ?? false;
    final overlay = await Permission.systemAlertWindow.isGranted;
    final notification = await Permission.notification.isGranted;

    setState(() {
      _usageStatsGranted = usageStats;
      _overlayGranted = overlay;
      _notificationGranted = notification;
      _isChecking = false;
    });

    if (_usageStatsGranted && _overlayGranted && _notificationGranted) {
      widget.onPermissionsGranted();
    }
  }

  Future<void> _requestUsageStatsPermission() async {
    try {
      await UsageStats.grantUsagePermission();
      await _checkAllPermissions();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error requesting permission: $e')),
        );
      }
    }
  }

  Future<void> _requestOverlayPermission() async {
    final status = await Permission.systemAlertWindow.request();
    if (status.isGranted) {
      await _checkAllPermissions();
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Overlay permission is required for the app to work'),
          ),
        );
      }
    }
  }

  Future<void> _requestNotificationPermission() async {
    final status = await Permission.notification.request();
    await _checkAllPermissions();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Permissions Required'),
        centerTitle: true,
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: _isChecking
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Icon(
                    Icons.security,
                    size: 80,
                    color: Colors.deepPurple,
                  ),
                  const SizedBox(height: 24),
                  const Text(
                    'Welcome to Extra Mind Time',
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'This app needs the following permissions to help you manage your app usage mindfully:',
                    style: TextStyle(fontSize: 16),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 32),
                  _buildPermissionCard(
                    icon: Icons.insert_chart,
                    title: 'Usage Stats Access',
                    description:
                        'Required to monitor when you open selected apps',
                    isGranted: _usageStatsGranted,
                    onRequest: _requestUsageStatsPermission,
                  ),
                  const SizedBox(height: 16),
                  _buildPermissionCard(
                    icon: Icons.layers,
                    title: 'Display Over Other Apps',
                    description:
                        'Required to show the mindful delay screen when you open selected apps',
                    isGranted: _overlayGranted,
                    onRequest: _requestOverlayPermission,
                  ),
                  const SizedBox(height: 16),
                  _buildPermissionCard(
                    icon: Icons.notifications,
                    title: 'Notifications',
                    description:
                        'Required to run the monitoring service in the background',
                    isGranted: _notificationGranted,
                    onRequest: _requestNotificationPermission,
                  ),
                  const SizedBox(height: 32),
                  if (_usageStatsGranted &&
                      _overlayGranted &&
                      _notificationGranted)
                    ElevatedButton(
                      onPressed: widget.onPermissionsGranted,
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        backgroundColor: Colors.deepPurple,
                        foregroundColor: Colors.white,
                      ),
                      child: const Text(
                        'Continue',
                        style: TextStyle(fontSize: 18),
                      ),
                    ),
                ],
              ),
            ),
    );
  }

  Widget _buildPermissionCard({
    required IconData icon,
    required String title,
    required String description,
    required bool isGranted,
    required VoidCallback onRequest,
  }) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  icon,
                  size: 32,
                  color: isGranted ? Colors.green : Colors.orange,
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Text(
                    title,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                Icon(
                  isGranted ? Icons.check_circle : Icons.cancel,
                  color: isGranted ? Colors.green : Colors.red,
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              description,
              style: const TextStyle(fontSize: 14, color: Colors.grey),
            ),
            if (!isGranted) ...[
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: onRequest,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.deepPurple,
                  foregroundColor: Colors.white,
                ),
                child: const Text('Grant Permission'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
