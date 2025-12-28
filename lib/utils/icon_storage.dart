import 'dart:io';
import 'package:path_provider/path_provider.dart';

class IconStorage {
  static const String _iconPrefix = 'app_icon_';

  static Future<String> saveIcon(String packageName, List<int> bytes) async {
    final directory = await getTemporaryDirectory();
    final sanitized = _sanitizePackageName(packageName);
    final file = File('${directory.path}/$_iconPrefix$sanitized.png');
    await file.writeAsBytes(bytes);
    return file.path;
  }

  static Future<List<int>?> loadIcon(String path) async {
    try {
      final file = File(path);
      if (await file.exists()) {
        return await file.readAsBytes();
      }
    } catch (_) {}
    return null;
  }

  static Future<void> cleanupUnusedIcons(Set<String> activePackages) async {
    try {
      final directory = await getTemporaryDirectory();
      final files = await directory
          .list()
          .where((f) => f is File)
          .cast<File>()
          .toList();

      for (final file in files) {
        final filename = file.path.split('/').last;
        if (filename.startsWith(_iconPrefix)) {
          final packageName = _extractPackageName(filename);
          if (!activePackages.contains(packageName)) {
            await file.delete();
          }
        }
      }
    } catch (_) {}
  }

  static String _sanitizePackageName(String packageName) {
    return packageName.replaceAll('.', '_').replaceAll('-', '_');
  }

  static String _extractPackageName(String filename) {
    final withoutPrefix = filename.replaceFirst(_iconPrefix, '');
    return withoutPrefix.replaceFirst('.png', '').replaceAll('_', '.');
  }
}
