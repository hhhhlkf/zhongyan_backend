import os

def generate_tile_coordinates(top_left_lon, top_left_lat, bottom_right_lon, bottom_right_lat,
                            cols, rows, output_folder):
    """
    生成图片切片的经纬度坐标文件
    
    参数:
    top_left_lat: 大图片左上角纬度
    top_left_lon: 大图片左上角经度
    bottom_right_lat: 大图片右下角纬度
    bottom_right_lon: 大图片右下角经度
    rows: 纵向切片数量
    cols: 横向切片数量
    output_folder: 输出文件夹路径
    """
    
    # 创建输出文件夹（如果不存在）
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
        print(f"创建文件夹: {output_folder}")
    
    # 计算每个切片的经纬度跨度
    lat_span = (top_left_lat - bottom_right_lat) / rows  # 纬度跨度（注意纬度是从大到小）
    lon_span = (bottom_right_lon - top_left_lon) / cols  # 经度跨度
    
    print(f"总经纬度范围: 纬度 {bottom_right_lat:.6f} 到 {top_left_lat:.6f}, 经度 {top_left_lon:.6f} 到 {bottom_right_lon:.6f}")
    print(f"切片大小: {rows}行 x {cols}列")
    print(f"每个切片跨度: 纬度 {lat_span:.6f}°, 经度 {lon_span:.6f}°")
    print("开始生成坐标文件...")
    
    # 生成每个切片的坐标文件
    for row in range(rows):
        for col in range(cols):
            # 计算当前切片的左上角坐标
            tile_top_lat = top_left_lat - row * lat_span
            tile_left_lon = top_left_lon + col * lon_span
            
            # 计算当前切片的右下角坐标
            tile_bottom_lat = top_left_lat - (row + 1) * lat_span
            tile_right_lon = top_left_lon + (col + 1) * lon_span
            
            # 生成文件名（格式：tile_行号_列号.txt，从0开始编号）
            filename = f"rgbP_{row:02d}{col:02d}.txt"
            filepath = os.path.join(output_folder, filename)
            
            # 写入坐标信息到文件
            with open(filepath, 'w', encoding='utf-8') as f:
                # 左上角经纬度
                f.write(f"{tile_left_lon:.6f} {tile_top_lat:.6f}\n")
                # 右下角经纬度
                f.write(f"{tile_right_lon:.6f} {tile_bottom_lat:.6f}")
            
            # 打印进度
            if (row * cols + col + 1) % 10 == 0 or (row * cols + col + 1) == rows * cols:
                print(f"已生成 {row * cols + col + 1}/{rows * cols} 个文件")
    
    print(f"完成！共生成了 {rows * cols} 个坐标文件在文件夹: {output_folder}")


# 使用示例
if __name__ == "__main__":
    # 示例参数
    top_left_lon = 112.804826     # 左上角经度
    top_left_lat = 29.42628      # 左上角纬度

    bottom_right_lon = 112.821721 # 右下角经度
    bottom_right_lat = 29.3924918  # 右下角纬度

    cols = 6                 # 横向切片数量
    rows = 12                 # 纵向切片数量

    output_folder = "./poly"  # 输出文件夹
    
    # 生成坐标文件
    generate_tile_coordinates(
        top_left_lon, top_left_lat,
        bottom_right_lon, bottom_right_lat,
        cols, rows, output_folder
    )
