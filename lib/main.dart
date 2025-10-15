import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:usage_stats/usage_stats.dart';
import 'dart:async';
import 'dart:convert';
import 'screens/permissions_screen.dart';
import 'screens/home_screen.dart';
import 'screens/delay_screen.dart';

// Global navigator key to show delay screen from anywhere
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MindfulTimeApp());
}

class MindfulTimeApp extends StatelessWidget {
  const MindfulTimeApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      navigatorKey: navigatorKey,
      title: 'Mindful Time',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const InitialScreen(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class InitialScreen extends StatefulWidget {
  const InitialScreen({super.key});

  @override
  State<InitialScreen> createState() => _InitialScreenState();
}

class _InitialScreenState extends State<InitialScreen> {
  bool _isLoading = true;
  bool _hasPermissions = false;

  @override
  void initState() {
    super.initState();
    _checkPermissions();
  }

  Future<void> _checkPermissions() async {
    setState(() => _isLoading = true);

    // Check if we have usage stats permission
    final usageStatsGranted = await UsageStats.checkUsagePermission() ?? false;

    // Check if we have overlay permission
    final overlayGranted = await Permission.systemAlertWindow.isGranted;

    setState(() {
      _hasPermissions = usageStatsGranted && overlayGranted;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    if (!_hasPermissions) {
      return PermissionsScreen(
        onPermissionsGranted: () {
          setState(() {
            _hasPermissions = true;
          });
        },
      );
    }

    return const HomeScreen();
  }
}
