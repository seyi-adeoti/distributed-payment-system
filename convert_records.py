import os, re

d = 'user-service/src/main/java/com/example/user/dtos'
for f in os.listdir(d):
    if not f.endswith('.java'): continue
    path = os.path.join(d, f)
    with open(path, 'r') as fp: content = fp.read()
    
    match = re.search(r'public record (\w+)\((.*?)\)(.*)', content, re.DOTALL)
    if match:
        name = match.group(1)
        args = match.group(2)
        
        fields = []
        if args.strip():
            for arg in args.split(','):
                arg = arg.strip()
                if arg: fields.append(f'    private {arg};')
        
        new_class = f'''import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {name} {{
{chr(10).join(fields)}
}}'''
        
        new_content = re.sub(r'public record.*', new_class, content, flags=re.DOTALL)
        with open(path, 'w') as fp: fp.write(new_content)
        print(f'Converted {f}')
