import 'package:flutter/material.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:installed_apps/app_info.dart';
import 'dart:convert';

class AppSelectionScreen extends StatefulWidget {
  final List<String> selectedApps;
  final Map<String, String> appNames;

  const AppSelectionScreen({
    super.key,
    required this.selectedApps,
    this.appNames = const {},
  });

  @override
  State<AppSelectionScreen> createState() => _AppSelectionScreenState();
}

class _AppSelectionScreenState extends State<AppSelectionScreen> {
  List<AppInfo> _installedApps = [];
  List<String> _selectedPackages = [];
  Map<String, String> _appNames = {};
  bool _isLoading = true;
  String _searchQuery = '';
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _selectedPackages = List.from(widget.selectedApps);
    _appNames = Map.from(widget.appNames);
    _loadInstalledApps();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadInstalledApps() async {
    setState(() => _isLoading = true);

    try {
      final apps = await InstalledApps.getInstalledApps(
        excludeSystemApps: false,
        withIcon: true,
      );

      // Debug: Print number of apps and how many have icons
      print('Loaded ${apps.length} apps');
      int appsWithIcons = apps.where((app) => app.icon != null).length;
      print('Apps with icons: $appsWithIcons');

      // Sort apps alphabetically by name
      apps.sort((a, b) => (a.name ?? '').compareTo(b.name ?? ''));

      setState(() {
        _installedApps = apps;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Error loading apps: $e')));
      }
    }
  }

  void _toggleAppSelection(String packageName) {
    setState(() {
      if (_selectedPackages.contains(packageName)) {
        _selectedPackages.remove(packageName);
      } else {
        _selectedPackages.add(packageName);
      }
    });
  }

  List<AppInfo> _getFilteredApps() {
    if (_searchQuery.isEmpty) {
      return _installedApps;
    }

    return _installedApps.where((app) {
      final appNameLower = (app.name ?? '').toLowerCase();
      final packageNameLower = (app.packageName ?? '').toLowerCase();
      final queryLower = _searchQuery.toLowerCase();
      return appNameLower.contains(queryLower) ||
          packageNameLower.contains(queryLower);
    }).toList();
  }

  void _saveSelection() {
    Navigator.pop(context, {
      'selectedApps': _selectedPackages,
      'appNames': _appNames,
    });
  }

  @override
  Widget build(BuildContext context) {
    final filteredApps = _getFilteredApps();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Select Apps to Monitor'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          TextButton.icon(
            onPressed: _saveSelection,
            icon: const Icon(Icons.check, color: Colors.white),
            label: Text(
              'Save (${_selectedPackages.length})',
              style: const TextStyle(color: Colors.white),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          // Search Bar
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: 'Search apps...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchQuery.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          setState(() {
                            _searchController.clear();
                            _searchQuery = '';
                          });
                        },
                      )
                    : null,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                filled: true,
                fillColor: Colors.grey[100],
              ),
              onChanged: (value) {
                setState(() => _searchQuery = value);
              },
            ),
          ),

          // Selection Summary
          if (_selectedPackages.isNotEmpty)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              color: Colors.deepPurple[50],
              child: Row(
                children: [
                  Icon(Icons.info_outline, color: Colors.deepPurple[700]),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      '${_selectedPackages.length} app${_selectedPackages.length == 1 ? '' : 's'} selected',
                      style: TextStyle(
                        color: Colors.deepPurple[700],
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  TextButton(
                    onPressed: () {
                      setState(() => _selectedPackages.clear());
                    },
                    child: const Text('Clear All'),
                  ),
                ],
              ),
            ),

          // Apps List
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : filteredApps.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.search_off,
                          size: 64,
                          color: Colors.grey[400],
                        ),
                        const SizedBox(height: 16),
                        Text(
                          _searchQuery.isEmpty
                              ? 'No apps found'
                              : 'No apps match your search',
                          style: TextStyle(
                            fontSize: 16,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ),
                  )
                : ListView.builder(
                    itemCount: filteredApps.length,
                    itemBuilder: (context, index) {
                      final app = filteredApps[index];
                      final packageName = app.packageName ?? '';
                      final isSelected = _selectedPackages.contains(
                        packageName,
                      );

                      Widget icon = const Icon(
                        Icons.android,
                        size: 40,
                        color: Colors.green,
                      );

                      // Debug: Check if icon is available
                      if (app.icon != null) {
                        icon = Image.memory(
                          app.icon!,
                          width: 40,
                          height: 40,
                          errorBuilder: (context, error, stackTrace) {
                            return const Icon(
                              Icons.android,
                              size: 40,
                              color: Colors.green,
                            );
                          },
                        );
                      }

                      return Card(
                        margin: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 4,
                        ),
                        elevation: isSelected ? 3 : 1,
                        color: isSelected
                            ? Colors.deepPurple[50]
                            : Colors.white,
                        child: ListTile(
                          leading: icon,
                          title: Text(
                            app.name ?? packageName,
                            style: TextStyle(
                              fontWeight: isSelected
                                  ? FontWeight.bold
                                  : FontWeight.normal,
                            ),
                          ),
                          subtitle: Text(
                            packageName,
                            style: const TextStyle(
                              fontSize: 12,
                              color: Colors.grey,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          trailing: Checkbox(
                            value: isSelected,
                            onChanged: (value) {
                              if (packageName.isNotEmpty) {
                                _toggleAppSelection(packageName);
                              }
                            },
                            activeColor: Colors.deepPurple,
                          ),
                          onTap: () {
                            if (packageName.isNotEmpty) {
                              _toggleAppSelection(packageName);
                            }
                          },
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
      floatingActionButton: _selectedPackages.isNotEmpty
          ? FloatingActionButton.extended(
              onPressed: () {
                // Update app names and icons for selected apps
                for (final app in _installedApps) {
                  final packageName = app.packageName ?? '';
                  if (_selectedPackages.contains(packageName) &&
                      app.name != null) {
                    _appNames[packageName] = app.name!;
                    // Store icon data as base64 string
                    if (app.icon != null) {
                      _appNames['${packageName}_icon'] = base64Encode(
                        app.icon!,
                      );
                    }
                  }
                }
                _saveSelection();
              },
              backgroundColor: Colors.deepPurple,
              icon: const Icon(Icons.check, color: Colors.white),
              label: const Text(
                'Save Selection',
                style: TextStyle(color: Colors.white),
              ),
            )
          : null,
    );
  }
}
