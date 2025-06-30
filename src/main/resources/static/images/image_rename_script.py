import os
import shutil
import re
from pathlib import Path

def process_images(input_folder, output_folder):
    """
    处理图片文件，根据命名规则重命名并复制到新路径
    
    参数:
    input_folder: 输入文件夹路径
    output_folder: 输出文件夹路径
    """
    
    # 创建输出文件夹（如果不存在）
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
        print(f"创建输出文件夹: {output_folder}")
    
    # 支持的图片格式
    supported_formats = {'.png', '.jpg', '.jpeg'}
    
    # 统计信息
    processed_count = 0
    skipped_count = 0
    error_count = 0
    
    print(f"开始处理文件夹: {input_folder}")
    print("=" * 50)
    
    # 遍历输入文件夹中的所有文件
    for filename in os.listdir(input_folder):
        # 获取文件的完整路径
        input_filepath = os.path.join(input_folder, filename)
        
        # 跳过文件夹
        if os.path.isdir(input_filepath):
            continue
        
        # 获取文件扩展名
        file_extension = Path(filename).suffix.lower()
        
        # 检查是否为支持的图片格式
        if file_extension not in supported_formats:
            continue
        
        try:
            # 解析文件名（不包含扩展名）
            filename_without_ext = Path(filename).stem
            
            # 检查文件名格式是否为AA_BB_CC
            parts = filename_without_ext.split('_')
            if len(parts) != 3:
                print(f"跳过文件（命名格式不符合AA_BB_CC）: {filename}")
                skipped_count += 1
                continue
            
            AA, BB, CC = parts
            original_AA = AA
            
            # 根据规则替换AA部分
            if 'D' in AA:
                AA = 'rgbO'
                print(f"处理文件: {filename}")
                print(f"  原AA部分: {original_AA} -> 新AA部分: {AA} (包含字母'd')")
            elif 'A' in AA:
                AA = 'rgb'
                print(f"处理文件: {filename}")
                print(f"  原AA部分: {original_AA} -> 新AA部分: {AA} (包含字母'a')")
            else:
                print(f"处理文件: {filename}")
                print(f"  AA部分无需更改: {AA}")
            
            # 构造新文件名
            new_filename = f"{AA}_{BB}{CC}{file_extension}"
            output_filepath = os.path.join(output_folder, new_filename)
            
            # 复制文件到新路径
            shutil.copy2(input_filepath, output_filepath)
            print(f"  输出文件: {new_filename}")
            print("-" * 30)
            
            processed_count += 1
            
        except Exception as e:
            print(f"处理文件时出错: {filename}")
            print(f"错误信息: {str(e)}")
            print("-" * 30)
            error_count += 1
    
    # 输出统计信息
    print("=" * 50)
    print("处理完成！")
    print(f"成功处理: {processed_count} 个文件")
    print(f"跳过文件: {skipped_count} 个")
    print(f"错误文件: {error_count} 个")
    print(f"输出文件夹: {output_folder}")

def process_images_with_preview(input_folder, output_folder, preview_only=False):
    """
    处理图片文件，支持预览模式
    
    参数:
    input_folder: 输入文件夹路径
    output_folder: 输出文件夹路径
    preview_only: 是否仅预览不执行实际操作
    """
    
    if not preview_only and not os.path.exists(output_folder):
        os.makedirs(output_folder)
    
    supported_formats = {'.png', '.jpg', '.jpeg'}
    changes = []
    
    print(f"{'预览模式' if preview_only else '执行模式'}: 处理文件夹 {input_folder}")
    print("=" * 60)
    
    # 扫描所有符合条件的文件
    for filename in os.listdir(input_folder):
        input_filepath = os.path.join(input_folder, filename)
        
        if os.path.isdir(input_filepath):
            continue
        
        file_extension = Path(filename).suffix.lower()
        if file_extension not in supported_formats:
            continue
        
        filename_without_ext = Path(filename).stem
        parts = filename_without_ext.split('_')
        
        if len(parts) != 3:
            continue
        
        AA, BB, CC = parts
        original_AA = AA
        new_AA = AA
        rule_applied = "无变化"
        
        # 应用重命名规则
        if 'D' in AA:
            new_AA = 'rgbO'
            rule_applied = "包含'd' -> rgbO"
        elif 'A' in AA:
            new_AA = 'rgb'
            rule_applied = "包含'a' -> rgb"
        
        new_filename = f"{new_AA}_{BB}_{CC}{file_extension}"
        
        changes.append({
            'original': filename,
            'new': new_filename,
            'rule': rule_applied,
            'original_AA': original_AA,
            'new_AA': new_AA
        })
    
    # 显示所有变化
    for i, change in enumerate(changes, 1):
        print(f"{i:3d}. {change['original']} -> {change['new']}")
        print(f"     规则: {change['rule']}")
        print()
    
    if preview_only:
        print(f"预览完成，共找到 {len(changes)} 个文件需要处理")
        return changes
    
    # 执行实际操作
    success_count = 0
    for change in changes:
        try:
            input_filepath = os.path.join(input_folder, change['original'])
            output_filepath = os.path.join(output_folder, change['new'])
            shutil.copy2(input_filepath, output_filepath)
            success_count += 1
        except Exception as e:
            print(f"复制文件失败: {change['original']} -> {e}")
    
    print(f"处理完成！成功处理 {success_count}/{len(changes)} 个文件")

# 使用示例
if __name__ == "__main__":
    # 示例路径
    input_folder = "./dongtinghu"     # 输入文件夹路径
    output_folder = "./res/rgb/process"   # 输出文件夹路径

    process_images(input_folder, output_folder)
